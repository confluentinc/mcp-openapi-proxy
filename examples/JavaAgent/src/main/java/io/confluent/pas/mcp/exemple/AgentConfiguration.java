package io.confluent.pas.mcp.exemple;

import io.confluent.pas.mcp.common.services.KafkaConfiguration;
import io.confluent.pas.mcp.proxy.frameworks.java.models.Key;
import io.confluent.pas.mcp.proxy.frameworks.java.SubscriptionHandler;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
public class AgentConfiguration {
    
    /**
     * Creates a SubscriptionHandler bean.
     * This bean is responsible for handling Kafka subscriptions for the Agent.
     *
     * @param kafkaConfiguration The Kafka configuration
     * @return SubscriptionHandler instance
     */
    @Bean
    public SubscriptionHandler<Key, AgentQuery, AgentResponse> subscriptionHandler(KafkaConfiguration kafkaConfiguration) {
        return new SubscriptionHandler<>(
                kafkaConfiguration,
                Key.class,
                AgentQuery.class,
                AgentResponse.class);
    }

}

