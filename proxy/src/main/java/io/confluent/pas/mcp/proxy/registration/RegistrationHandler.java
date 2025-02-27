package io.confluent.pas.mcp.proxy.registration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.pas.mcp.common.services.Schemas;
import io.confluent.pas.mcp.proxy.registration.schemas.RegistrationSchemas;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.AllArgsConstructor;
import lombok.Getter;
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

    private final static TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private final static ObjectMapper MAPPER = new ObjectMapper();

    @Getter
    private final Schemas.Registration registration;
    @Getter
    private final RegistrationSchemas schemas;
    private final RequestResponseHandler requestResponseHandler;

    public RegistrationHandler(Schemas.Registration registration,
                               SchemaRegistryClient schemaRegistryClient,
                               RequestResponseHandler requestResponseHandler) throws RestClientException, IOException {
        this.requestResponseHandler = requestResponseHandler;
        this.registration = registration;
        this.schemas = new RegistrationSchemas(schemaRegistryClient, registration);
    }

    /**
     * Register the tool with the server
     *
     * @param mcpServer the server to register with
     * @return a mono that completes when the registration is complete
     */
    public Mono<Void> register(McpAsyncServer mcpServer) {
        if (registration instanceof Schemas.ResourceRegistration rcsRegistration) {
            final McpSchema.Annotations annotations = new McpSchema.Annotations(
                    List.of(McpSchema.Role.ASSISTANT, McpSchema.Role.USER),
                    1.0
            );

            McpSchema.Resource registration = new McpSchema.Resource(
                    rcsRegistration.getUrl(),
                    rcsRegistration.getName(),
                    rcsRegistration.getDescription(),
                    rcsRegistration.getMimeType(),
                    annotations
            );

            log.info("Registering resource {}", rcsRegistration.getName());

            final McpServerFeatures.AsyncResourceRegistration resourceRegistration = new McpServerFeatures.AsyncResourceRegistration(registration,
                    (toolArguments) -> Mono.create(sink -> sendResourceRequest(
                            rcsRegistration,
                            toolArguments,
                            sink)));

            return mcpServer.addResource(resourceRegistration);
        }


        final McpSchema.Tool tool = new McpSchema.Tool(
                registration.getName(),
                registration.getDescription(),
                schemas.getRequestSchema().getSchema());

        log.info("Registering tool {}", registration.getName());
        final McpServerFeatures.AsyncToolRegistration toolRegistration = new McpServerFeatures.AsyncToolRegistration(tool,
                (toolArguments) -> Mono.create(sink -> sendToolRequest(toolArguments, sink)));

        return mcpServer.addTool(toolRegistration);
    }

    /**
     * Unregister the tool from the server
     *
     * @param mcpServer the server to unregister from
     * @return a mono that completes when the un-registration is complete
     */
    public Mono<Void> unregister(McpAsyncServer mcpServer) {
        return mcpServer.removeTool(registration.getName());
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
                    .sendRequestResponse(registration,
                            schemas,
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
    protected void sendToolRequest(Map<String, Object> arguments, MonoSink<McpSchema.CallToolResult> sink) {
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

    /**
     * Send a request to the resource
     *
     * @param rcsRegistration the resource registration
     * @param request         the read resource request
     * @param sink            the sink to send the response to
     */
    protected void sendResourceRequest(Schemas.ResourceRegistration rcsRegistration,
                                       McpSchema.ReadResourceRequest request,
                                       MonoSink<McpSchema.ReadResourceResult> sink) {
        final Map<String, Object> arguments = MAPPER.convertValue(request, MAP_TYPE);

        sendRequest(arguments).subscribe(response -> {
            final Schemas.ResourceResponse.ResponseType responseType = Schemas.ResourceResponse.ResponseType.fromValue(response.get("type").toString());

            final McpSchema.ResourceContents content;
            if (responseType == Schemas.ResourceResponse.ResponseType.BLOB) {
                Schemas.TextResourceResponse resource = MAPPER.convertValue(response, Schemas.TextResourceResponse.class);
                content = new McpSchema.TextResourceContents(
                        resource.getUri(),
                        resource.getMimeType(),
                        resource.getText());
            } else {
                Schemas.BlobResourceResponse resource = MAPPER.convertValue(response, Schemas.BlobResourceResponse.class);
                content = new McpSchema.BlobResourceContents(
                        resource.getUri(),
                        resource.getMimeType(),
                        resource.getBlob());
            }
            
            sink.success(new McpSchema.ReadResourceResult(List.of(content)));
        });
    }
}
