package io.confluent.pas.mcp.proxy.registration.internal;

import io.confluent.pas.mcp.proxy.registration.kafka.ConsumerService;
import io.confluent.pas.mcp.common.services.KafkaConfiguration;
import io.confluent.pas.mcp.common.services.Schemas;
import io.confluent.pas.mcp.common.utils.AutoReadWriteLock;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.*;

/**
 * Handle responses from Kafka topics
 */
@Slf4j
public class KafkaResponseHandler implements Closeable {

    /**
     * Registration Handler
     */
    public interface ResponseHandler {
        void handle(Map<String, Object> response);
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

    private static class Response extends HashMap<String, Object> {
    }

    private static class ResponseKey extends HashMap<String, Object> {
    }

    private final Map<String, RegistrationItem> responseHandlers = new ConcurrentHashMap<>();
    private final AutoReadWriteLock lock = new AutoReadWriteLock();

    private final ConsumerService<ResponseKey, Response> consumerService;

    public KafkaResponseHandler(KafkaConfiguration kafkaConfiguration) {
        this.consumerService = new ConsumerService<>(
                kafkaConfiguration,
                ResponseKey.class,
                Response.class);
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

        try {
            lock.writeLockAndExecute(() -> {
                final RegistrationItem registrationItem = responseHandlers
                        .computeIfAbsent(responseTopic, k -> {
                            log.info("Creating new registration item for topic: {}", responseTopic);
                            consumerService.subscribeForEvent(responseTopic, this::handleResponse);
                            return new RegistrationItem(registration, new ConcurrentHashMap<>());
                        });
                if (registrationItem.responseHandlers.containsKey(correlationId)) {
                    log.warn("Handler already registered for correlation id: {}", correlationId);
                } else {
                    registrationItem.responseHandlers.put(correlationId, handler);
                }
            });
        } catch (Exception e) {
            log.error("Error registering response handler for topic: {} ", responseTopic, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        consumerService.close();
    }

    /**
     * Handle a response
     *
     * @param topic   The topic
     * @param key     The key
     * @param message The message
     */
    private void handleResponse(String topic, Map<String, Object> key, Map<String, Object> message) {
        try {
            lock.writeLockAndExecute(() -> {
                final RegistrationItem registrationItem = responseHandlers.get(topic);
                final Schemas.Registration registration = registrationItem.registration;
                final String correlationIdKey = registration.getCorrelationIdFieldName();
                final Map<String, ResponseHandler> handlers = registrationItem.responseHandlers;

                if (handlers != null && !handlers.isEmpty()) {
                    if (!key.containsKey(correlationIdKey)) {
                        log.warn("No correlation id in key: {}", key);
                        return;
                    }

                    final String correlationId = (String) key.get(correlationIdKey);
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
            });
        } catch (Exception e) {
            log.error("Error processing message", e);
        }
    }
}