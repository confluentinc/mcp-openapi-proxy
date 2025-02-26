package io.confluent.pas.mcp.proxy.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientFactory;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.json.JsonSchemaProvider;
import io.confluent.pas.mcp.common.services.KafkaConfigration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Authentication manager for handling user authentication.
 * This class uses a schema registry client to authenticate users and caches authenticated users.
 */
@Slf4j
public class AuthManager implements ReactiveAuthenticationManager {

    private static final String SCHEMA_REGISTRY_URL = "schema.registry.url";
    private static final String BASIC_AUTH_CREDENTIALS_SOURCE = "basic.auth.credentials.source";
    private static final String USER_INFO = "USER_INFO";
    private static final String SCHEMA_REGISTRY_BASIC_AUTH_USER_INFO = "schema.registry.basic.auth.user.info";

    private final KafkaConfigration.SR schemaRegistryConfig;
    private final Cache<String, UserAuthenticated> usersAuthenticated;

    /**
     * Constructs a new AuthManager instance with the provided configuration.
     *
     * @param schemaRegistryConfig the schema registry configuration
     * @param cacheSize            the size of the cache
     * @param cacheExpiry          the expiry time for cache entries in seconds
     */
    public AuthManager(KafkaConfigration.SR schemaRegistryConfig, int cacheSize, int cacheExpiry) {
        this.schemaRegistryConfig = schemaRegistryConfig;
        this.usersAuthenticated = Caffeine.newBuilder()
                .maximumSize(cacheSize)
                .expireAfterWrite(Duration.ofSeconds(cacheExpiry))
                .build();
    }

    /**
     * Authenticates the user based on the provided authentication object.
     *
     * @param authentication the authentication object containing user credentials
     * @return a Mono emitting the authenticated user or an error if authentication fails
     */
    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        return Mono.create(sink -> {
            String principal = authentication.getPrincipal().toString();
            log.info("Authenticating user: {}", principal);

            UserAuthenticated userAuthenticated = usersAuthenticated.getIfPresent(principal);
            if (userAuthenticated != null && userAuthenticated.getCredentials().equals(authentication.getCredentials())) {
                sink.success(userAuthenticated);
                return;
            }

            userAuthenticated = new UserAuthenticated(authentication, false);
            Map<String, Object> config = getSchemaRegistryConfig(principal, authentication.getCredentials().toString());

            try (SchemaRegistryClient schemaRegistryClient = createSchemaRegistryClient(config)) {
                schemaRegistryClient.getAllSubjects();
                userAuthenticated.setAuthenticated(true);
                usersAuthenticated.put(principal, userAuthenticated);
                sink.success(userAuthenticated);
                log.info("User authenticated: {}", principal);
            } catch (Exception e) {
                handleAuthenticationException(e, principal, authentication, sink);
            }
        });
    }

    /**
     * Creates a SchemaRegistryClient instance with the provided configuration.
     *
     * @param config the schema registry configuration map
     * @return the SchemaRegistryClient instance
     */
    private SchemaRegistryClient createSchemaRegistryClient(Map<String, Object> config) {
        return SchemaRegistryClientFactory.newClient(
                List.of(schemaRegistryConfig.url),
                1,
                List.of(new JsonSchemaProvider(), new AvroSchemaProvider()),
                config,
                new HashMap<>()
        );
    }

    /**
     * Handles exceptions that occur during authentication.
     *
     * @param e              the exception that occurred
     * @param principal      the username of the user being authenticated
     * @param authentication the authentication object
     * @param sink           the MonoSink to emit the result
     */
    private void handleAuthenticationException(Exception e, String principal, Authentication authentication, MonoSink<Authentication> sink) {
        if (e instanceof RestClientException restClientException && restClientException.getErrorCode() == 401) {
            log.error("User not authenticated: {}", principal);
            authentication.setAuthenticated(false);
            usersAuthenticated.invalidate(principal);
            sink.success(authentication);
        } else {
            log.error("Failed to authenticate user", e);
            sink.error(e);
        }
    }

    /**
     * Creates the schema registry configuration map with user credentials.
     *
     * @param username the username
     * @param password the password
     * @return the schema registry configuration map
     */
    private Map<String, Object> getSchemaRegistryConfig(String username, String password) {
        Map<String, Object> config = new HashMap<>();
        config.put(SCHEMA_REGISTRY_URL, schemaRegistryConfig.url);
        config.put(BASIC_AUTH_CREDENTIALS_SOURCE, USER_INFO);
        config.put(SCHEMA_REGISTRY_BASIC_AUTH_USER_INFO, username + ":" + password);
        return config;
    }
}