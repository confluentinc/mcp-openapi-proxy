// AgentRequestHandler.java
package io.confluent.pas.mcp.proxy.frameworks.python;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kafka.schemaregistry.json.JsonSchemaUtils;
import io.confluent.pas.mcp.proxy.frameworks.java.Request;
import io.confluent.pas.mcp.proxy.frameworks.java.models.Key;
import io.confluent.pas.mcp.proxy.frameworks.python.exceptions.AgentException;
import io.confluent.pas.mcp.proxy.frameworks.python.models.AgentGenericRequest;
import io.confluent.pas.mcp.proxy.frameworks.python.models.AgentGenericResponse;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Handles requests and responses between MCP tools and the agent system.
 * This class is responsible for processing tool requests, managing responses,
 * and handling data transformations between different formats.
 */
@Slf4j
@RequiredArgsConstructor
public class AgentRequestHandler {
    /**
     * Client for asynchronous MCP communication
     */
    private final McpAsyncClient mcpAsyncClient;
    /**
     * ObjectMapper for JSON serialization/deserialization
     */
    private final ObjectMapper mapper;
    /**
     * Configuration containing tool settings
     */
    private final AgentConfiguration config;

    /**
     * Processes incoming tool requests by calling the appropriate MCP tool
     * and handling the response.
     *
     * @param request The incoming request containing key, generic request data, and JSON payload
     */
    public void handleRequest(Request<Key, AgentGenericRequest, JsonNode> request) {
        mcpAsyncClient.callTool(new McpSchema.CallToolRequest(config.getTool().getName(), request.getRequest()))
                .flatMap(result -> processToolResponse(result, request))
                .doOnError(error -> log.error("Error processing request", error))
                .block();
    }

    /**
     * Processes the tool's response by converting it to the expected format
     * and sending it back through the request channel.
     *
     * @param result  The result from the MCP tool call
     * @param request The original request for context
     * @return A Mono completing when the response is processed
     * @throws AgentException if response processing fails or unexpected response type is received
     */
    private Mono<Void> processToolResponse(McpSchema.CallToolResult result, Request<Key, AgentGenericRequest, JsonNode> request) {
        if (!(result.content().getFirst() instanceof McpSchema.TextContent textContent)) {
            return Mono.error(new AgentException("Unexpected response type"));
        }

        try {
            var response = mapper.readValue(textContent.text(), AgentGenericResponse.class);
            var jsonNode = mapper.valueToTree(response);
            var envelope = JsonSchemaUtils.envelope(config.getTool().getOutput_schema(), jsonNode);
            return request.respond(envelope);
        } catch (JsonProcessingException e) {
            return Mono.error(new AgentException("Error processing tool response", e));
        }
    }
}