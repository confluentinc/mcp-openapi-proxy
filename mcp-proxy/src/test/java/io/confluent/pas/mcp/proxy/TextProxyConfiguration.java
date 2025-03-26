package io.confluent.pas.mcp.proxy;

import io.confluent.pas.mcp.common.services.KafkaConfiguration;
import org.springframework.boot.web.servlet.DynamicRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;

@Configuration
@Testcontainers
public class TextProxyConfiguration {
    private static final String KAFKA_IMAGE = "apache/kafka:3.7.0";

    @Container
    private static final KafkaContainer kafka = new KafkaContainer(KAFKA_IMAGE)
            .withStartupTimeout(Duration.ofMinutes(2))
            .withStartupAttempts(3);

    @Bean
    public KafkaContainer kafka() {
        return kafka;
    }

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("kafka.broker-servers", kafka::getBootstrapServers);
    }
//
//    @Bean
//    public KafkaConfiguration kafkaConfiguration(KafkaContainer kafka) {
//        return new KafkaConfiguration() {
//            @Override
//            public String brokerServers() {
//                return kafka.getBootstrapServers();
//            }
//
//            @Override
//            public String schemaRegistryUrl() {
//                return "mock://localhost:8081";
//            }
//
//            @Override
//            public String applicationId() {
//                return "proxy";
//            }
//
//            @Override
//            public String saslJaasConfig() {
//                return null;
//            }
//
//            @Override
//            public String schemaRegistryBasicAuthUserInfo() {
//                return null;
//            }
//        };
//    }
}
