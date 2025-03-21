package io.confluent.pas.mcp.proxy.registration.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import io.confluent.pas.mcp.common.services.KafkaConfiguration;
import io.confluent.pas.mcp.common.services.Schemas;
import io.confluent.pas.mcp.common.utils.AutoReadWriteLock;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Handle responses from Kafka topics
 */
@Slf4j
public class KafkaResponseHandler implements Closeable {

    /**
     * Registration Handler
     */
    public interface ResponseHandler {
        void handle(JsonNode response);
    }

    /**
     * Registration Item
     *
     * @param registration     the registration
     * @param responseHandlers the response handlers
     */
    private record RegistrationItem(Schemas.Registration registration,
                                    Map<String, ResponseHandler> responseHandlers) {
    }

    private static class ResponseKey extends HashMap<String, Object> {
    }

    private final Map<String, RegistrationItem> responseHandlers = new ConcurrentHashMap<>();
    private final AutoReadWriteLock lock = new AutoReadWriteLock();

    private final ConsumerService<ResponseKey, JsonNode> consumerService;

    public KafkaResponseHandler(KafkaConfiguration kafkaConfiguration) {
        this.consumerService = new ConsumerService<>(
                kafkaConfiguration,
                ResponseKey.class,
                JsonNode.class,
                this::handleResponse);
    }

    public void addRegistrations(Collection<Schemas.Registration> registrations) {
        consumerService.subscribe(registrations
                .stream()
                .map(Schemas.Registration::getResponseTopicName)
                .collect(Collectors.toList()));
    }

    /**
     * Register a response handler for a topic
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

        // Check if the did subscribe to the given registration
        if (!consumerService.isSubscribed(responseTopic)) {
            // If not make sure we do
            consumerService.subscribe(responseTopic);
        }

        // Lower case the correlation id
        final String key = correlationId.toLowerCase();

        try {
            lock.writeLockAndExecute(() -> {
                final RegistrationItem registrationItem = responseHandlers
                        .computeIfAbsent(responseTopic, k -> {
                            log.info("Creating new registration item for topic: {}", responseTopic);
                            return new RegistrationItem(registration, new ConcurrentHashMap<>());
                        });
                if (registrationItem.responseHandlers.containsKey(key)) {
                    log.warn("Handler already registered for correlation id: {}", key);
                } else {
                    registrationItem.responseHandlers.put(key, handler);
                }
            });
        } catch (Exception e) {
            log.error("Error registering response handler for topic: {} ", responseTopic, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
        consumerService.close();
    }

    /**
     * Handle a response
     *
     * @param topic   The topic
     * @param key     The key
     * @param message The message
     */
    private void handleResponse(String topic, Map<String, Object> key, JsonNode message) {
        try {
            lock.writeLockAndExecute(() -> {
                log.info("Handling response for topic: {}", topic);

                if (!responseHandlers.containsKey(topic)) {
                    log.info("No handlers for topic: {}", topic);
                    return;
                }

                final RegistrationItem registrationItem = responseHandlers.get(topic);
                final Schemas.Registration registration = registrationItem.registration;
                final String correlationIdKey = registration.getCorrelationIdFieldName();
                final Map<String, ResponseHandler> handlers = registrationItem.responseHandlers;

                if (handlers != null && !handlers.isEmpty()) {
                    if (!key.containsKey(correlationIdKey)) {
                        log.warn("No correlation id in key: {}", key);
                        return;
                    }

                    final String correlationId = ((String) key.get(correlationIdKey)).toLowerCase();
                    final ResponseHandler handler = handlers.get(correlationId);
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
                } else {
                    log.warn("No handler for topic: {}", topic);
                }

                log.info("Response handled for topic: {}", topic);
            });
        } catch (Exception e) {
            log.error("Error processing message", e);
        }
    }
}