package io.confluent.pas.mcp.proxy.security;

import io.confluent.pas.mcp.common.services.KafkaConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
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

    @Value("${authentication.cache-size}")
    private int cacheSize = 100;
    @Value("${authentication.cache-expiry-in-second}")
    private int cacheExpiry = 3600;
    @Value("${authentication.enabled}")
    private boolean authenticationEnabled = true;

    /**
     * Configures the security filter chain for the server.
     * This method sets up basic authentication and disables CSRF protection.
     *
     * @param http               the ServerHttpSecurity instance
     * @param kafkaConfiguration the kafka configuration
     * @return the configured SecurityWebFilterChain
     */
    @Bean
    @ConditionalOnProperty(prefix = "mcp.server", name = "mode", havingValue = "sse")
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
                                                            KafkaConfiguration kafkaConfiguration) {
        if (!authenticationEnabled) {
            return http.authorizeExchange((exchanges) -> exchanges.anyExchange().permitAll())
                    .csrf(ServerHttpSecurity.CsrfSpec::disable)
                    .headers(Customizer.withDefaults())
//                    .authenticationManager(new AuthManager(schemaRegistryConfig, cacheSize, cacheExpiry))
                    .build();
        }

        return http.authorizeExchange((exchanges) -> exchanges.anyExchange().authenticated())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic((httpBasicSpec) -> {
                    httpBasicSpec.authenticationManager(new AuthManager(kafkaConfiguration, cacheSize, cacheExpiry));
                })
                .formLogin((httpBasicSpec) -> {
                    httpBasicSpec.authenticationManager(new AuthManager(kafkaConfiguration, cacheSize, cacheExpiry));
                })
                .authenticationManager(new AuthManager(kafkaConfiguration, cacheSize, cacheExpiry))
                .build();
    }
}