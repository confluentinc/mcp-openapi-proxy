package io.confluent.pas.mcp.proxy.rest;

import io.confluent.pas.mcp.proxy.registration.RegistrationCoordinator;
import io.confluent.pas.mcp.proxy.registration.RegistrationHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Component
public class ToolRestController {

    private final static ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<>() {
    };
    private final RegistrationCoordinator coordinator;

    @Autowired
    public ToolRestController(RegistrationCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    /**
     * Process a request for a tool
     *
     * @param request the request
     * @return the response
     */
    public Mono<ServerResponse> processRequest(ServerRequest request) {
        final String toolName = request.pathVariable("toolName");

        if (!coordinator.isRegistered(toolName)) {
            log.error("Tool {} is not registered", toolName);
            return ServerResponse.badRequest().bodyValue(Map.of("message", "Tool " + toolName + " is not registered"));
        }

        final RegistrationHandler handler = coordinator.getRegistrationHandler(toolName);

        return Mono.from(request.bodyToMono(MAP_TYPE))
                .doOnNext(arguments -> log.info("Received request for tool {}", toolName))
                .flatMap(handler::sendRequest)
                .doOnError(e -> log.error("Error sending request to tool {}", toolName, e))
                .flatMap(response -> ServerResponse.ok().bodyValue(response));
    }
}
