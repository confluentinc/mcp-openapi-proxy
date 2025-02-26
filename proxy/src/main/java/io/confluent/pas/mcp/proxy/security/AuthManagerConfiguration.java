package io.confluent.pas.mcp.proxy.security;

import io.confluent.pas.mcp.common.services.KafkaConfigration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Configuration class for setting up the authentication manager.
 * This class configures the security filter chain for the application.
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity
public class AuthManagerConfiguration {

    private KafkaConfigration.SR schemaRegistry;

    @Value("${authentication.cache-size}")
    private int cacheSize = 100;
    @Value("${authentication.cache-expiry-in-second}")
    private int cacheExpiry = 3600;

    /**
     * Configures the security filter chain for the server.
     * This method sets up basic authentication and disables CSRF protection.
     *
     * @param http                 the ServerHttpSecurity instance
     * @param schemaRegistryConfig the SchemaRegistry configuration
     * @return the configured SecurityWebFilterChain
     */
    @Bean
    @ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "sse")
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
                                                            KafkaConfigration.SR schemaRegistryConfig) {
        http.authorizeExchange((exchanges) -> exchanges.anyExchange().authenticated())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic((httpBasicSpec) -> {
                    httpBasicSpec.authenticationManager(new AuthManager(schemaRegistryConfig, cacheSize, cacheExpiry));
                })
                .formLogin((httpBasicSpec) -> {
                    httpBasicSpec.authenticationManager(new AuthManager(schemaRegistryConfig, cacheSize, cacheExpiry));
                });

        return http.build();
    }
}