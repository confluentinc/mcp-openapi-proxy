package io.confluent.pas.mcp.proxy.registration.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.pas.mcp.common.services.Schemas;
import io.confluent.pas.mcp.proxy.registration.RequestResponseHandler;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ToolHandlerTest {

    @Mock
    private Schemas.Registration registration;

    @Mock
    private SchemaRegistryClient schemaRegistryClient;

    @Mock
    private RequestResponseHandler requestResponseHandler;

    @Mock
    private McpAsyncServer mcpServer;

    private ToolHandler toolHandler;

    @BeforeEach
    void setUp() throws RestClientException, IOException {
        MockitoAnnotations.openMocks(this);

        when(schemaRegistryClient.getLatestSchemaMetadata(any())).thenReturn(new SchemaMetadata(
                1,
                1,
                "{\"type\":\"string\"}"
        ));

        toolHandler = new ToolHandler(registration, schemaRegistryClient, requestResponseHandler);
    }

    @Test
    void testRegister() {
        when(registration.getName()).thenReturn("test-tool");
        when(registration.getDescription()).thenReturn("Test Tool");

        McpSchema.Tool tool = new McpSchema.Tool("test-tool", "Test Tool", "null");
        McpServerFeatures.AsyncToolRegistration toolRegistration = new McpServerFeatures.AsyncToolRegistration(tool, null);

        when(mcpServer.addTool(argThat(p -> p.tool().name().equals(toolRegistration.tool().name()))))
                .thenReturn(Mono.empty());

        Mono<Void> result = toolHandler.register(mcpServer);
        assertNotNull(result);
        verify(mcpServer).addTool(any());
    }

    @Test
    void testUnregister() {
        when(registration.getName()).thenReturn("test-tool");

        when(mcpServer.removeTool("test-tool")).thenReturn(Mono.empty());

        Mono<Void> result = toolHandler.unregister(mcpServer);
        assertNotNull(result);
        verify(mcpServer).removeTool("test-tool");
    }

    @Test
    void testSendRequest() throws ExecutionException, InterruptedException {
        Map<String, Object> arguments = Map.of("key", "value");

        when(requestResponseHandler.sendRequestResponse(any(), any(), anyString(), anyMap()))
                .thenReturn(Mono.just(mock(JsonNode.class)));

        Mono<JsonNode> result = toolHandler.sendRequest(arguments);
        assertNotNull(result);
        verify(requestResponseHandler).sendRequestResponse(any(), any(), anyString(), anyMap());
    }

    @Test
    void testSendToolRequest() throws ExecutionException, InterruptedException {
        Map<String, Object> arguments = Map.of("key", "value");
        MonoSink<McpSchema.CallToolResult> sink = mock(MonoSink.class);

        when(requestResponseHandler.sendRequestResponse(any(), any(), anyString(), anyMap()))
                .thenReturn(Mono.just(mock(JsonNode.class)));

        toolHandler.sendToolRequest(arguments, sink);
        verify(requestResponseHandler).sendRequestResponse(any(), any(), anyString(), anyMap());
    }
}