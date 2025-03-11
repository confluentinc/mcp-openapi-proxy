package io.confluent.pas.mcp.proxy.frameworks.python;

import io.confluent.pas.mcp.proxy.frameworks.python.exceptions.AgentException;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

/**
 * Handles the initialization process for MCP (Model Context Protocol) tools.
 * This class is responsible for discovering and validating tools through the MCP client,
 * with built-in retry mechanisms for reliability.
 */
@Slf4j
public class AgentInitializer {
    /** Client for asynchronous MCP communications */
    private final McpAsyncClient mcpAsyncClient;
    /** Configuration containing tool specifications */
    private final AgentConfiguration config;

    /**
     * Creates a new initializer instance.
     *
     * @param mcpAsyncClient The async client for MCP communication
     * @param config The configuration containing tool specifications
     */
    public AgentInitializer(McpAsyncClient mcpAsyncClient, AgentConfiguration config) {
        this.mcpAsyncClient = mcpAsyncClient;
        this.config = config;
    }

    /**
     * Initializes the tool handling system.
     * This method:
     * 1. Lists available tools through MCP
     * 2. Validates and finds the configured tool
     * 3. Implements retry logic for reliability
     *
     * @return A Mono containing the validated tool handler
     * @throws AgentException if tool validation fails
     */
    public Mono<AgentToolHandler> initialize() {
        return mcpAsyncClient.listTools()
                .flatMap(tools -> findAndValidateTool(tools.tools()))
                .retryWhen(Retry.fixedDelay(60, Duration.ofSeconds(1))
                        .filter(error -> error instanceof AgentException)
                        .doBeforeRetry(retrySignal ->
                                log.warn("Retrying initialization attempt {}", retrySignal.totalRetries()))
                )
                .doOnSuccess(result -> log.info("Agent initialization completed"))
                .doOnError(err -> log.error("Error initializing agent", err));
    }

    /**
     * Finds and validates a specific tool from the available tools list.
     *
     * @param tools List of available MCP tools
     * @return A Mono containing the validated tool handler
     * @throws AgentException if no tools are found or the requested tool is not available
     */
    private Mono<AgentToolHandler> findAndValidateTool(List<McpSchema.Tool> tools) {
        if (tools.isEmpty()) {
            return Mono.error(new AgentException("No tools found"));
        }

        String toolName = config.getTool().getName();
        return tools.stream()
                .filter(t -> t.name().equals(toolName))
                .findFirst()
                .map(tool -> new AgentToolHandler(tool, toolName))
                .map(Mono::just)
                .orElseGet(() -> Mono.error(new AgentException("Tool not found: " + toolName)));
    }
}