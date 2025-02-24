package io.confluent.pas.mcp.demo.mcp.connection;

import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import io.modelcontextprotocol.spec.ClientMcpTransport;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.reactive.function.client.WebClient;


/**
 * Represents a connection to an MCP server.
 */
@Getter
@AllArgsConstructor
public class McpSseConnection extends McpAbstractConnection {

    private String url;

    @Override
    protected ClientMcpTransport getTransport() {
        WebClient.Builder webClientBuilder = WebClient
                .builder()
                .baseUrl(url);

        return new WebFluxSseClientTransport(webClientBuilder);
    }
}
