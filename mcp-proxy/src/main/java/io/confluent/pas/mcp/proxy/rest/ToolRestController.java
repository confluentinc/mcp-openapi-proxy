package io.confluent.pas.mcp.proxy.rest;

import com.fasterxml.jackson.databind.JsonNode;
import io.confluent.pas.mcp.common.services.Schemas;
import io.confluent.pas.mcp.proxy.registration.RegistrationCoordinator;
import io.confluent.pas.mcp.proxy.registration.RegistrationHandler;
import io.confluent.pas.mcp.proxy.registration.handlers.ResourceHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
@Component
public class ToolRestController {

    // Type reference for deserializing request body to a Map
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<>() {
    };

    // Coordinator for managing registrations
    private final RegistrationCoordinator coordinator;

    // Map to store resource handlers by URL
    private final Map<String, ResourceHandler> resourceHandlers = new HashMap<>();

    public ToolRestController(RegistrationCoordinator coordinator) {
        this.coordinator = coordinator;

        coordinator.getAllRegistrationHandlers()
                .stream()
                .filter(handler -> handler instanceof ResourceHandler)
                .map(handler -> (ResourceHandler) handler)
                .forEach(handler -> {
                    final Schemas.ResourceRegistration registration = handler.getRegistration();
                    final String url = registration.getUrl();
                    resourceHandlers.put(url, handler);
                });
    }

    /**
     * Process a request for a tool
     *
     * @param request the request
     * @return the response
     */
    @SuppressWarnings("unchecked")
    public Mono<ServerResponse> processRequest(ServerRequest request) {
        final String toolName = request.pathVariable("toolName");

        // Check if the tool is registered
        if (!coordinator.isRegistered(toolName)) {
            log.error("Tool {} is not registered", toolName);
            return ServerResponse.badRequest().bodyValue(Map.of("message", "Tool " + toolName + " is not registered"));
        }

        // Get the registration handler for the tool
        final RegistrationHandler<Map<String, Object>, JsonNode> handler =
                (RegistrationHandler<Map<String, Object>, JsonNode>) coordinator.getRegistrationHandler(toolName);

        // Process the request body and send it to the handler
        return request.bodyToMono(MAP_TYPE)
                .doOnNext(arguments -> log.info("Received request for tool {}", toolName))
                .flatMap(handler::sendRequest)
                .doOnError(e -> log.error("Error sending request to tool {}", toolName, e))
                .flatMap(response -> ServerResponse.ok().bodyValue(response));
    }

    /**
     * Process a resource request
     *
     * @param request the request
     * @return the response
     */
    public Mono<ServerResponse> processResourceRequest(ServerRequest request) {
        final List<String> urlParts = getUrlParts(request);

        // Get the appropriate handler for the resource request
        final RegistrationHandler<Schemas.ResourceRequest, Schemas.ResourceResponse> handler = getHandler(urlParts);
        if (handler == null) {
            log.error("No handler found for {}", request.path());
            return ServerResponse.badRequest().bodyValue(Map.of("message", "No handler found for " + request.path()));
        }

        final Schemas.ResourceRequest resourceRequest = new Schemas.ResourceRequest(StringUtils.join(urlParts, "/"));

        // Send the resource request to the handler
        return handler.sendRequest(resourceRequest)
                .doOnError(e -> log.error("Error sending request to resource {}", request.path(), e))
                .flatMap(this::createServerResponse);
    }

    /**
     * Get the URL parts from the request
     *
     * @param request the request
     * @return the URL parts
     */
    private List<String> getUrlParts(ServerRequest request) {
        return Stream.of(request.path().split("/"))
                .filter(StringUtils::isNotEmpty)
                .skip(1)
                .toList();
    }

    /**
     * Create a server response from the resource response
     *
     * @param response the resource response
     * @return the server response
     */
    private Mono<ServerResponse> createServerResponse(Schemas.ResourceResponse response) {
        final MediaType mediaType = MediaType.parseMediaType(response.getMimeType());
        final String value = (response instanceof Schemas.BlobResourceResponse) ?
                ((Schemas.BlobResourceResponse) response).getBlob() :
                ((Schemas.TextResourceResponse) response).getText();

        return ServerResponse.ok().contentType(mediaType).bodyValue(value);
    }

    /**
     * Get the handler for the given URL parts
     *
     * @param urlParts the URL parts
     * @return the handler
     */
    private RegistrationHandler<Schemas.ResourceRequest, Schemas.ResourceResponse> getHandler(List<String> urlParts) {
        return resourceHandlers.keySet().stream()
                .filter(key -> isMatchingHandler(key, urlParts))
                .map(resourceHandlers::get)
                .findFirst()
                .orElse(null);
    }

    /**
     * Check if the given key matches the URL parts
     *
     * @param key      the key
     * @param urlParts the URL parts
     * @return true if the key matches the URL parts
     */
    private boolean isMatchingHandler(String key, List<String> urlParts) {
        final List<String> parts = Stream.of(key.split("/"))
                .filter(StringUtils::isNotEmpty)
                .toList();
        if (urlParts.size() != parts.size()) {
            return false;
        }

        return Stream.iterate(0, i -> i + 1)
                .limit(urlParts.size())
                .allMatch(i -> (parts.get(i).startsWith("{") && parts.get(i).endsWith("}")) ||
                        parts.get(i).equals(urlParts.get(i)));
    }
}