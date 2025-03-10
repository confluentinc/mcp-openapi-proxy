package io.confluent.pas.mcp.proxy.frameworks.python;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.pas.mcp.common.services.KafkaConfiguration;
import io.confluent.pas.mcp.common.services.Schemas;
import io.confluent.pas.mcp.proxy.frameworks.java.SubscriptionHandler;
import io.confluent.pas.mcp.proxy.frameworks.java.models.Key;
import io.confluent.pas.mcp.proxy.frameworks.python.exceptions.AgentException;
import io.confluent.pas.mcp.proxy.frameworks.python.models.AgentGenericRequest;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

/**
 * Core component managing MCP (Model Context Protocol) communication between Python tools and Kafka.
 * This Spring-managed component handles tool initialization, subscription management, and request processing.
 */
@Slf4j
@Component
public class Agent {
    /**
     * Shared ObjectMapper instance for JSON operations
     */
    private final static ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Client for asynchronous MCP communications
     */
    private final McpAsyncClient mcpAsyncClient;
    /**
     * Configuration for agent behavior and tools
     */
    private final AgentConfiguration agentConfiguration;
    /**
     * Kafka-specific configuration
     */
    private final KafkaConfiguration kafkaConfiguration;
    /**
     * Handler for processing tool requests
     */
    private final AgentRequestHandler requestHandler;
    /**
     * Handler for managing Kafka subscriptions
     */
    private SubscriptionHandler<Key, AgentGenericRequest, JsonNode> subscriptionHandler;

    /**
     * Creates a new Agent instance with required configurations.
     * Initializes the MCP client with stdio transport and sets up request handling.
     *
     * @param kafkaConfiguration Configuration for Kafka connectivity
     * @param agentConfiguration Agent-specific configuration including tool settings
     */
    @Autowired
    public Agent(KafkaConfiguration kafkaConfiguration, AgentConfiguration agentConfiguration) {
        this.kafkaConfiguration = kafkaConfiguration;
        this.agentConfiguration = agentConfiguration;

        var transport = new StdioClientTransport(ServerParameters.builder(agentConfiguration.getCommand())
                .args(agentConfiguration.getArguments())
                .env(System.getenv())
                .build());

        this.mcpAsyncClient = McpClient.async(transport)
                .clientInfo(new McpSchema.Implementation("mcp-client-proxy", "0.1.0"))
                .capabilities(McpSchema.ClientCapabilities.builder().roots(true).build())
                .build();

        this.requestHandler = new AgentRequestHandler(mcpAsyncClient, MAPPER, agentConfiguration);
    }

    /**
     * Initializes the agent after construction.
     * Performs MCP client initialization and starts tool handler setup.
     */
    @PostConstruct
    public void init() {
        log.info("Initializing agent");
        mcpAsyncClient.initialize().block();

        new AgentInitializer(mcpAsyncClient, agentConfiguration)
                .initialize()
                .doOnSuccess(this::setupSubscriptionHandler)
                .subscribe();
    }

    /**
     * Cleans up resources when the agent is shutting down.
     * Ensures the subscription handler is properly stopped.
     */
    @PreDestroy
    public void destroy() {
        if (subscriptionHandler != null) {
            subscriptionHandler.close();
        }
    }

    /**
     * Sets up subscription handling for a specific tool.
     * Creates necessary registrations and starts the subscription process.
     *
     * @param handler The tool handler to set up subscriptions for
     */
    private void setupSubscriptionHandler(AgentToolHandler handler) {
        var registration = new Schemas.Registration(
                handler.tool().name(),
                handler.tool().description(),
                handler.tool().name() + "_request",
                handler.tool().name() + "_response");

        this.subscriptionHandler = new SubscriptionHandler<>(
                kafkaConfiguration,
                Key.class,
                AgentGenericRequest.class,
                JsonNode.class);

        subscriptionHandler.subscribeWith(
                registration,
                createInputSchema(handler.tool()),
                agentConfiguration.getTool().getOutput_schema(),
                requestHandler::handleRequest);
    }

    /**
     * Creates a JSON schema for the tool's input parameters.
     *
     * @param tool The MCP tool to create the schema for
     * @return A JsonSchema representing the tool's input parameters
     * @throws AgentException if schema serialization fails
     */
    private JsonSchema createInputSchema(McpSchema.Tool tool) {
        try {
            return new JsonSchema(MAPPER.writeValueAsString(tool.inputSchema()));
        } catch (JsonProcessingException e) {
            throw new AgentException("Error serializing input schema.", e);
        }
    }
}