package io.confluent.pas.mcp.proxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.StdioServerTransport;
import io.modelcontextprotocol.server.transport.WebFluxSseServerTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.ServerMcpTransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;

/**
 * Configuration class for the MCP server.
 * This class sets up the security configuration, transports, and server instance for the MCP server.
 */
@Slf4j
@Configuration
public class MCPServerConfig {
    private final static String MESSAGE_ENDPOINT = "/mcp/message";

    @Value("${mcp.server.name}")
    private String serverName;

    @Value("${mcp.server.version}")
    private String serverVersion;

    /**
     * Creates a WebFluxSseServerTransport bean if the transport mode is set to SSE.
     * This transport is used for handling Server-Sent Events (SSE).
     *
     * @return the WebFluxSseServerTransport instance
     */
    @Bean
    @ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "sse")
    public WebFluxSseServerTransport sseServerTransport() {
        return new WebFluxSseServerTransport(new ObjectMapper(), MESSAGE_ENDPOINT);
    }

    /**
     * Creates a RouterFunction bean for the SSE transport.
     * This router function defines the HTTP endpoints for the SSE transport.
     *
     * @param transport the WebFluxSseServerTransport instance
     * @return the configured RouterFunction
     */
    @Bean
    @ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "sse")
    public RouterFunction<?> mcpRouterFunction(WebFluxSseServerTransport transport) {
        return transport.getRouterFunction();
    }

    /**
     * Creates a StdioServerTransport bean if the transport mode is set to stdio.
     * This transport is used for handling standard input/output communication.
     *
     * @return the StdioServerTransport instance
     */
    @Bean
    @ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "stdio")
    public StdioServerTransport stdioServerTransport() {
        return new StdioServerTransport();
    }

    /**
     * Creates an asynchronous MCP server instance.
     * This method initializes the server with the specified transport and server information.
     *
     * @param transport the ServerMcpTransport instance
     * @return the configured McpAsyncServer instance
     */
    @Bean
    public McpAsyncServer mcpAsyncServer(ServerMcpTransport transport) {
        log.info("Starting MCP server {} version {} with transport: {} ",
                serverName,
                serverVersion,
                transport.getClass().getSimpleName());

        return McpServer.async(transport)
                .serverInfo(serverName, serverVersion)
                .capabilities(McpSchema.ServerCapabilities
                        .builder()
                        .tools(true)
                        .resources(true, true)
                        .logging()
                        .build())
                .tools()
                .build();
    }
}