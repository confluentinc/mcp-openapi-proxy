package io.confluent.pas.mcp.proxy.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.pas.mcp.common.services.KafkaConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.ArrayDeque;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthManagerTest {

    @Mock
    private KafkaConfiguration kafkaConfiguration;

    @Mock
    private Authentication authentication;

    @Mock
    private SchemaRegistryClient schemaRegistryClient;

    private AuthManager authManager;

    private Cache<String, UserAuthenticated> usersAuthenticated;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        usersAuthenticated = Caffeine.newBuilder().maximumSize(100).build();
        authManager = new AuthManager(kafkaConfiguration, usersAuthenticated);
        authManager = spy(authManager);
        doReturn(schemaRegistryClient).when(authManager).createSchemaRegistryClient(anyMap());
    }

    @Test
    void testAuthenticateUserSuccessfully() throws RestClientException, IOException {
        String principal = "user";
        String credentials = "password";
        when(authentication.getPrincipal()).thenReturn(principal);
        when(authentication.getCredentials()).thenReturn(credentials);

        when(schemaRegistryClient.getAllSubjects()).thenReturn(new ArrayDeque<>());

        Mono<Authentication> result = authManager.authenticate(authentication);

        StepVerifier.create(result)
                .expectNextMatches(auth -> auth.isAuthenticated() && auth.getPrincipal().equals(principal))
                .verifyComplete();

        verify(schemaRegistryClient, times(1)).getAllSubjects();
        assertNotNull(usersAuthenticated.getIfPresent(principal));
    }

    @Test
    void testAuthenticateUserWithInvalidCredentials() throws RestClientException, IOException {
        String principal = "user";
        String credentials = "wrong_password";
        when(authentication.getPrincipal()).thenReturn(principal);
        when(authentication.getCredentials()).thenReturn(credentials);

        doThrow(new RestClientException("Unauthorized", 401, 401)).when(schemaRegistryClient).getAllSubjects();

        Mono<Authentication> result = authManager.authenticate(authentication);

        StepVerifier.create(result)
                .expectNextMatches(auth -> !auth.isAuthenticated() && auth.getPrincipal().equals(principal))
                .verifyComplete();

        verify(schemaRegistryClient, times(1)).getAllSubjects();
        assertNull(usersAuthenticated.getIfPresent(principal));
    }

    @Test
    void testAuthenticateUserWithException() throws RestClientException, IOException {
        String principal = "user";
        String credentials = "password";
        when(authentication.getPrincipal()).thenReturn(principal);
        when(authentication.getCredentials()).thenReturn(credentials);

        doThrow(new RuntimeException("Unexpected error")).when(schemaRegistryClient).getAllSubjects();

        Mono<Authentication> result = authManager.authenticate(authentication);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(schemaRegistryClient, times(1)).getAllSubjects();
        assertNull(usersAuthenticated.getIfPresent(principal));
    }
}