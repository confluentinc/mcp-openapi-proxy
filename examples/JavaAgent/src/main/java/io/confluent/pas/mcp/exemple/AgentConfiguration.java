package io.confluent.pas.mcp.exemple;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.pas.mcp.common.services.KafkaConfigration;
import io.confluent.pas.mcp.proxy.frameworks.java.models.Key;
import io.confluent.pas.mcp.proxy.frameworks.java.models.Registration;
import io.confluent.pas.mcp.proxy.frameworks.java.SubscriptionHandler;
import io.confluent.pas.mcp.proxy.frameworks.java.kafka.KafkaAdminClientConfig;
import io.confluent.pas.mcp.proxy.frameworks.java.kafka.TopicConfiguration;
import io.confluent.pas.mcp.proxy.frameworks.java.kafka.TopicManagement;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties("registration")
public class AgentConfiguration {

    private String registrationTopic;
    private String applicationId;
    private String requestTopic;
    private String responseTopic;

    /**
     * Creates a TopicManagement bean.
     * This bean is responsible for managing Kafka topics.
     *
     * @param topicConfiguration     The topic configuration
     * @param kafkaAdminClientConfig The Kafka admin client configuration
     * @param schemaRegistryClient   The schema registry client
     * @return TopicManagement instance
     */
    @Bean
    public TopicManagement topicManagement(TopicConfiguration topicConfiguration,
                                           KafkaAdminClientConfig kafkaAdminClientConfig,
                                           SchemaRegistryClient schemaRegistryClient) {
        return new TopicManagement(topicConfiguration, kafkaAdminClientConfig, schemaRegistryClient);
    }

    /**
     * Creates a SubscriptionHandler bean.
     * This bean is responsible for handling Kafka subscriptions for the Agent.
     *
     * @param topicManagement   The topic management
     * @param kafkaConfigration The Kafka configuration
     * @return SubscriptionHandler instance
     */
    @Bean
    public SubscriptionHandler<Key, AgentQuery, AgentResponse> subscriptionHandler(
            TopicManagement topicManagement,
            KafkaConfigration kafkaConfigration) {
        return new SubscriptionHandler<>(
                applicationId,
                topicManagement,
                kafkaConfigration,
                registrationTopic,
                Key.class,
                AgentQuery.class,
                AgentResponse.class);
    }

    /**
     * Creates a Registration bean.
     * This bean contains the registration details for the Agent.
     *
     * @return Registration instance
     */
    @Bean
    public Registration registration() {
        return new Registration(
                applicationId,
                "This agent return sentiments of a human request.",
                requestTopic,
                responseTopic);
    }

}

