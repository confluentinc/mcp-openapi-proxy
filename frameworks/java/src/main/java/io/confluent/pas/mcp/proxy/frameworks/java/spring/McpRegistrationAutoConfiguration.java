package io.confluent.pas.mcp.proxy.frameworks.java.spring;

import io.confluent.pas.mcp.common.services.Schemas;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Spring Auto-configuration class for MCP (Model Control Plane) agent registration.
 * Provides automatic configuration for registering agents within the MCP ecosystem by
 * creating a Registration bean with the necessary properties.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "agent", name = "name")
public class McpRegistrationAutoConfiguration {

    /**
     * The name of the agent to be registered.
     * Injected from the 'agent.name' property.
     */
    @Value("${agent.name}")
    private String name;

    /**
     * Description of the agent's capabilities and purpose.
     * Injected from the 'registration.description' property.
     */
    @Value("${agent.description}")
    private String agentDescription;

    /**
     * Kafka topic name where the agent will consume requests from.
     * Injected from the 'registration.request-topic' property.
     */
    @Value("${agent.request-topic}")
    private String requestTopic;

    /**
     * Kafka topic name where the agent will produce responses to.
     * Injected from the 'registration.response-topic' property.
     */
    @Value("${agent.response-topic}")
    private String responseTopic;

    /**
     * Field name used for request-response correlation.
     * Defaults to the standard correlation ID field name if not specified.
     * Injected from the 'registration.correlation-id' property.
     */
    @Value("${agent.correlation-id:" + Schemas.Registration.CORRELATION_ID_FIELD_NAME + "}")
    private String correlationIdFieldName;

    /**
     * Creates a Registration bean for the agent if one hasn't been defined.
     * This bean contains all the necessary information for registering the agent
     * in the MCP ecosystem.
     *
     * @return A Registration instance configured with the agent's properties
     */
    @Bean
    @ConditionalOnMissingBean
    public Schemas.Registration getRegistration() {
        return Schemas.Registration.builder()
                .name(name)
                .description(agentDescription)
                .requestTopicName(requestTopic)
                .responseTopicName(responseTopic)
                .correlationIdFieldName(correlationIdFieldName)
                .build();
    }
}