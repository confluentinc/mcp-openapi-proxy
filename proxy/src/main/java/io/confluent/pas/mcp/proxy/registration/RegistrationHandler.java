package io.confluent.pas.mcp.proxy.registration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.pas.mcp.proxy.registration.models.Registration;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Handle the registration for a tool
 */
@Slf4j
@AllArgsConstructor
public class RegistrationHandler {

    private final static ObjectMapper MAPPER = new ObjectMapper();

    private final Registration registrationHolder;
    private final RequestResponseHandler requestResponseHandler;

    public RegistrationHandler(Registration registration,
                               SchemaRegistryClient schemaRegistryClient,
                               RequestResponseHandler requestResponseHandler) throws RestClientException, IOException {
        this.requestResponseHandler = requestResponseHandler;
        this.registrationHolder = registration;

        // Make sure the schema is up to date
        registration.updateSchema(schemaRegistryClient);
    }

    /**
     * Register the tool with the server
     *
     * @param mcpServer the server to register with
     * @return a mono that completes when the registration is complete
     */
    public Mono<Void> register(McpAsyncServer mcpServer) {
        McpSchema.Tool tool = new McpSchema.Tool(
                registrationHolder.getName(),
                registrationHolder.getDescription(),
                registrationHolder.getRequestSchema().getSchema());

        final McpServerFeatures.AsyncToolRegistration toolRegistration = new McpServerFeatures.AsyncToolRegistration(tool,
                (toolArguments) -> Mono.create(sink -> sendRequest(toolArguments, sink)));

        return mcpServer.addTool(toolRegistration);
    }

    /**
     * Unregister the tool from the server
     *
     * @param mcpServer the server to unregister from
     * @return a mono that completes when the un-registration is complete
     */
    public Mono<Void> unregister(McpAsyncServer mcpServer) {
        return mcpServer.removeTool(registrationHolder.getName());
    }

    /**
     * Send a request to the tool
     *
     * @param arguments the arguments to send
     * @return the response
     */
    public Mono<Map<String, Object>> sendRequest(Map<String, Object> arguments) {
        final String correlationId = UUID.randomUUID().toString();

        try {
            //
            return requestResponseHandler
                    .sendRequestResponse(registrationHolder,
                            correlationId,
                            arguments);
        } catch (ExecutionException | InterruptedException e) {
            log.error("Failed to send request", e);
            return Mono.error(e);
        }
    }

    /**
     * Send a request to the tool
     *
     * @param arguments the arguments to send
     * @param sink      the sink to send the response to
     */
    protected void sendRequest(Map<String, Object> arguments, MonoSink<McpSchema.CallToolResult> sink) {
        sendRequest(arguments).subscribe(response -> {
            // Serialize the response
            try {
                final String result = MAPPER.writeValueAsString(response);
                sink.success(new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(result)),
                        false));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize response", e);
                sink.error(e);
            }
        });
    }
}
