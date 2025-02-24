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
 * Configuration for the MCP server
 */
@Slf4j
@Configuration
public class MCPServerConfig {
    private final static String MESSAGE_ENDPOINT = "/mcp/message";

    @Value("${mcp.server.name}")
    private String serverName;

    @Value("${mcp.server.version}")
    private String serverVersion;

    // SSE transport
    @Bean
    @ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "sse")
    public WebFluxSseServerTransport sseServerTransport() {
        return new WebFluxSseServerTransport(new ObjectMapper(), MESSAGE_ENDPOINT);
    }

    // Router function for SSE transport used by Spring WebFlux to start an HTTP
    // server.
    @Bean
    @ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "sse")
    public RouterFunction<?> mcpRouterFunction(WebFluxSseServerTransport transport) {
        return transport.getRouterFunction();
    }

    @Bean
    @ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "stdio")
    public StdioServerTransport stdioServerTransport() {
        return new StdioServerTransport();
    }

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
                        .logging()
                        .build())
                .tools()
                .build();
    }

}
