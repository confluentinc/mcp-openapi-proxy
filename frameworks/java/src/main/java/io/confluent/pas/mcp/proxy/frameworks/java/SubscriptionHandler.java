package io.confluent.pas.mcp.proxy.frameworks.java;

import io.confluent.pas.mcp.common.services.ConsumerService;
import io.confluent.pas.mcp.common.services.ProducerService;
import io.confluent.pas.mcp.common.services.RegistrationService;
import io.confluent.pas.mcp.common.services.Schemas;
import io.confluent.pas.mcp.common.services.KafkaConfiguration;
import io.confluent.pas.mcp.proxy.frameworks.java.kafka.TopicManagement;
import io.confluent.pas.mcp.proxy.frameworks.java.models.Key;
import lombok.extern.slf4j.Slf4j;

/**
 * SubscriptionHandler class that handles the subscription to a registration.
 * This class manages the lifecycle of Kafka consumers and producers, and handles
 * the registration and subscription of topics for processing requests and responses.
 *
 * @param <K>   Key type
 * @param <REQ> Request type
 * @param <RES> Response type
 */
@Slf4j
public class SubscriptionHandler<K extends Key, REQ, RES> {

    /**
     * RequestHandler interface for handling incoming requests.
     *
     * @param <K>   Key type
     * @param <REQ> Request type
     * @param <RES> Response type
     */
    public interface RequestHandler<K, REQ, RES> {
        void onRequest(Request<K, REQ, RES> request);
    }

    private final RegistrationService<Schemas.RegistrationKey, Schemas.Registration> registrationService;
    private final ProducerService<K, RES> responseService;
    private final ConsumerService<K, REQ> requestService;
    private final TopicManagement topicManagement;
    private final Class<K> keyClass;
    private final Class<REQ> requestClass;
    private final Class<RES> responseClass;

    /**
     * Constructor for SubscriptionHandler.
     *
     * @param kafkaConfigration The Kafka configuration
     * @param keyClass          The class type of the key
     * @param requestClass      The class type of the request
     * @param responseClass     The class type of the response
     */
    public SubscriptionHandler(KafkaConfiguration kafkaConfigration,
                               Class<K> keyClass,
                               Class<REQ> requestClass,
                               Class<RES> responseClass) {
        this.topicManagement = new TopicManagement(kafkaConfigration);
        this.responseService = new ProducerService<>(kafkaConfigration);

        this.requestService = new ConsumerService<>(
                kafkaConfigration,
                keyClass,
                requestClass);

        this.registrationService = new RegistrationService<>(
                kafkaConfigration,
                Schemas.RegistrationKey.class,
                Schemas.Registration.class);

        this.keyClass = keyClass;
        this.requestClass = requestClass;
        this.responseClass = responseClass;
    }

    /**
     * Starts the registration service.
     */
    public void start() {
        registrationService.start();
    }

    /**
     * Start the service
     */
    public void stop() {
        requestService.stop();
    }

    /**
     * Subscribes to a registration and handles incoming requests.
     *
     * @param registration Registration to use for the subscription
     * @param handler      RequestHandler to handle the request
     * @throws SubscriptionException if there is an error during subscription
     */
    public void subscribeWith(Schemas.Registration registration,
                              RequestHandler<K, REQ, RES> handler) throws SubscriptionException {
        log.info("Subscribing for registration: {}", registration.getName());

        // First we create the topic for the request/response
        try {
            topicManagement.createTopic(registration.getRequestTopicName(), keyClass, requestClass);
            topicManagement.createTopic(registration.getResponseTopicName(), keyClass, responseClass);
        } catch (Exception e) {
            log.error("Failed to create topic", e);
            throw new SubscriptionException("Failed to create topic", e);
        }

        // Register the capability
        final Schemas.RegistrationKey registrationKey = new Schemas.RegistrationKey(registration.getName());
        if (!registrationService.isRegistered(registrationKey)) {
            log.info("Registering: {}", registration.getName());
            registrationService.register(registrationKey, registration);
        } else {
            log.info("Already registered: {}", registration.getName());
        }

        // Add subscription to the request service
        requestService.subscribeForEvent(
                registration.getRequestTopicName(),
                (topic, key, request) -> {
                    final Request<K, REQ, RES> requestWrapper = new Request<>(
                            key,
                            request,
                            registration,
                            responseService);
                    handler.onRequest(requestWrapper);
                });
    }
}
