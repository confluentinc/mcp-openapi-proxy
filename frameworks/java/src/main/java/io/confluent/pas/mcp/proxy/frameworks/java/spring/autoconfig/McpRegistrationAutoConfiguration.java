package io.confluent.pas.mcp.proxy.frameworks.java.spring.autoconfig;

import io.confluent.pas.mcp.common.services.Schemas;
import io.confluent.pas.mcp.proxy.frameworks.java.spring.mcp.AsyncMcpToolCallbackProvider;
import io.modelcontextprotocol.client.McpAsyncClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Auto-configuration class for MCP (Model Control Protocol) agent registration.
 * Provides configuration for registering agents in the MCP ecosystem.
 * Only activated when the 'agent.name' property is present.
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "agent", name = "name")
public class McpRegistrationAutoConfiguration {

    /**
     * Name identifier for the agent
     */
    @Value("${agent.name}")
    private String name;

    /**
     * Human-readable description of the agent's purpose and capabilities
     */
    @Value("${agent.description}")
    private String agentDescription;

    /**
     * Kafka topic name where the agent listens for incoming requests
     */
    @Value("${agent.request-topic}")
    private String requestTopic;

    /**
     * Kafka topic name where the agent publishes responses
     */
    @Value("${agent.response-topic}")
    private String responseTopic;

    /**
     * Field name for request-response correlation, defaults to standard name
     */
    @Value("${agent.correlation-id:" + Schemas.Registration.CORRELATION_ID_FIELD_NAME + "}")
    private String correlationIdFieldName;

    /**
     * Comma-separated list of tool names to be denied access
     */
    @Value("${agent.deny-tools:#{null}}")
    private String deniedTools;

    /**
     * Creates a ToolCallbackProvider bean for handling MCP tool interactions.
     * Configures tool access restrictions if deny-tools are specified.
     *
     * @param registration   Registration configuration for the agent
     * @param mcpAsyncClient Async MCP client instance
     * @return Configured ToolCallbackProvider instance
     */
    @Bean
    @ConditionalOnBean(McpAsyncClient.class)
    public ToolCallbackProvider getToolCallbackProvider(Schemas.Registration registration, McpAsyncClient mcpAsyncClient) {
        final AsyncMcpToolCallbackProvider provider = new AsyncMcpToolCallbackProvider(registration, mcpAsyncClient);

        if (!StringUtils.isEmpty(deniedTools)) {
            final List<String> tools = List.of(deniedTools.split(","));
            return provider.denis(tools);
        }

        return provider;
    }

    /**
     * Creates a Registration bean with the agent's configuration.
     * Contains all necessary information for registering the agent in MCP.
     *
     * @return Configured Registration instance
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

//    @Bean
//    public AgentRegistrar agentRegistrar(KafkaConfiguration kafkaConfiguration, ApplicationContext applicationContext) {
//        return new AgentRegistrar(kafkaConfiguration, applicationContext);
//    }
}