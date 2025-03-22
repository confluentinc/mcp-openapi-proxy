package io.confluent.pas.mcp.proxy.registration.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import io.confluent.pas.mcp.common.services.KafkaConfiguration;
import io.confluent.pas.mcp.common.services.Schemas;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Handle responses from Kafka topics.
 */
@Slf4j
public class ConsumerService implements Closeable {

    /**
     * Registration Handler.
     */
    public interface ResponseHandler {
        void handle(JsonNode response);
    }

    /**
     * Registration Item.
     *
     * @param registration     the registration
     * @param responseHandlers the response handlers
     */
    public record RegistrationItem(Schemas.Registration registration,
                                   Map<String, ResponseHandler> responseHandlers) {
    }

    @Getter
    private final Map<String, RegistrationItem> responseHandlers = new ConcurrentHashMap<>();
    private final Consumer<JsonNode, JsonNode> consumer;

    public ConsumerService(KafkaConfiguration kafkaConfiguration) {
        this.consumer = new Consumer<>(
                kafkaConfiguration,
                JsonNode.class,
                JsonNode.class,
                this::handleResponse);
    }

    public ConsumerService(Consumer<JsonNode, JsonNode> consumer) {
        this.consumer = consumer;
    }

    public void addRegistrations(Collection<Schemas.Registration> registrations) {
        consumer.subscribe(registrations
                .stream()
                .map(Schemas.Registration::getResponseTopicName)
                .collect(Collectors.toList()));
    }

    /**
     * Register a response handler for a topic.
     *
     * @param registration  the registration
     * @param correlationId the correlation id
     * @param handler       the handler
     */
    public void registerResponseHandler(Schemas.Registration registration,
                                        String correlationId,
                                        ResponseHandler handler) {
        final String responseTopic = registration.getResponseTopicName();
        log.info("Registering response handler for topic: {}", responseTopic);

        if (!consumer.isSubscribed(responseTopic)) {
            consumer.subscribe(responseTopic);
        }

        final String key = correlationId.toLowerCase();

        responseHandlers.compute(responseTopic, (topic, registrationItem) -> {
            if (registrationItem == null) {
                log.info("Creating new registration item for topic: {}", responseTopic);
                registrationItem = new RegistrationItem(registration, new ConcurrentHashMap<>());
            }
            if (registrationItem.responseHandlers.containsKey(key)) {
                log.warn("Handler already registered for correlation id: {}", key);
            } else {
                registrationItem.responseHandlers.put(key, handler);
            }
            return registrationItem;
        });
    }

    @Override
    public void close() throws IOException {
        consumer.close();
    }

    /**
     * Handle a response.
     *
     * @param topic   The topic
     * @param key     The key
     * @param message The message
     */
    void handleResponse(String topic, JsonNode key, JsonNode message) {
        log.info("Handling response for topic: {}", topic);

        RegistrationItem registrationItem = responseHandlers.get(topic);
        if (registrationItem == null) {
            log.info("No handlers for topic: {}", topic);
            return;
        }

        String correlationIdKey = registrationItem.registration.getCorrelationIdFieldName();
        Map<String, ResponseHandler> handlers = registrationItem.responseHandlers;

        if (!key.has(correlationIdKey)) {
            log.warn("No correlation id in key: {}", key);
            return;
        }

        String correlationId = key.get(correlationIdKey).asText().toLowerCase();
        ResponseHandler handler = handlers.get(correlationId);
        if (handler != null) {
            try {
                handler.handle(message);
            } catch (Exception e) {
                log.error("Error handling message", e);
            }
            handlers.remove(correlationId);
        } else {
            log.warn("No handler for correlation id: {}", correlationId);
        }

        log.info("Response handled for topic: {}", topic);
    }
}