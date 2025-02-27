package io.confluent.pas.mcp.exemple;

import io.confluent.pas.mcp.common.services.Schemas;
import io.confluent.pas.mcp.proxy.frameworks.java.Request;
import io.confluent.pas.mcp.proxy.frameworks.java.SubscriptionHandler;
import io.confluent.pas.mcp.proxy.frameworks.java.models.Key;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ResourceAgent {

    private final SubscriptionHandler<Key, Schemas.ResourceRequest, Schemas.TextResourceResponse> subscriptionHandler;
    private final Schemas.ResourceRegistration registration;

    @Autowired
    public ResourceAgent(SubscriptionHandler<Key, Schemas.ResourceRequest, Schemas.TextResourceResponse> subscriptionHandler,
                         Schemas.ResourceRegistration registration) {
        this.subscriptionHandler = subscriptionHandler;
        this.registration = registration;
    }

    /**
     * Initializes the agent by starting the subscription handler and setting up the assistant.
     */
    @PostConstruct
    public void init() {
        // Start the subscription handler
        subscriptionHandler.start();

        // Subscribe using the registration information and handle requests with the onRequest method
        subscriptionHandler.subscribeWith(registration, this::onRequest);
    }

    /**
     * Cleans up resources by stopping the subscription handler.
     */
    @PreDestroy
    public void destroy() {
        // Stop the subscription handler
        subscriptionHandler.stop();
    }

    /**
     * Handles incoming requests by processing the query and responding with the sentiment analysis result.
     *
     * @param request The incoming request containing the query.
     */
    private void onRequest(Request<Key, Schemas.ResourceRequest, Schemas.TextResourceResponse> request) {
        log.info("Received request: {}", request.getRequest().getUri());

        request.respond(new Schemas.TextResourceResponse(
                        request.getRequest().getUri(),
                        registration.getMimeType(),
                        "{ \"message\": \"Hello, World!\" }"
                ))
                .doOnError(e -> log.error("Failed to respond to request", e))
                .block();
    }
}
