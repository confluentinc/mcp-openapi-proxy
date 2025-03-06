package io.confluent.pas.mcp.exemple;

import io.confluent.pas.mcp.common.services.KafkaConfiguration;
import io.confluent.pas.mcp.common.services.Schemas;
import io.confluent.pas.mcp.common.utils.UriTemplate;
import io.confluent.pas.mcp.proxy.frameworks.java.Request;
import io.confluent.pas.mcp.proxy.frameworks.java.SubscriptionHandler;
import io.confluent.pas.mcp.proxy.frameworks.java.models.Key;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Agent class responsible for delivering resources.
 */
@Slf4j
@Component
public class ResourceAgent {

    private final SubscriptionHandler<Key, Schemas.ResourceRequest, Schemas.TextResourceResponse> subscriptionHandler;
    private final Schemas.ResourceRegistration registration;
    private final UriTemplate template;

    /**
     * Constructor to initialize the ResourceAgent with required dependencies.
     *
     * @param configuration Kafka configuration for the agent.
     * @param registration  Contains registration details for the resource.
     */
    @Autowired
    public ResourceAgent(KafkaConfiguration configuration, Schemas.ResourceRegistration registration) {
        this.subscriptionHandler = new SubscriptionHandler<>(
                configuration,
                Key.class,
                Schemas.ResourceRequest.class,
                Schemas.TextResourceResponse.class);
        this.registration = registration;
        this.template = new UriTemplate(registration.getUrl());
    }

    /**
     * Initializes the agent by starting the subscription handler and setting up the subscription.
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
     * Handles incoming requests by processing the query and responding with a message.
     *
     * @param request The incoming request containing the query.
     */
    private void onRequest(Request<Key, Schemas.ResourceRequest, Schemas.TextResourceResponse> request) {
        log.info("Received request: {}", request.getRequest().getUri());

        // Extract values from the URI using the template
        final Map<String, Object> values = this.template.match(request.getRequest().getUri());

        // Respond to the request with a message containing the client_id
        request.respond(new Schemas.TextResourceResponse(
                        request.getRequest().getUri(),
                        registration.getMimeType(),
                        "{ \"message\": \"Hello, " + values.get("client_id") + "!\" }"
                ))
                .doOnError(e -> log.error("Failed to respond to request", e))
                .block();
    }
}