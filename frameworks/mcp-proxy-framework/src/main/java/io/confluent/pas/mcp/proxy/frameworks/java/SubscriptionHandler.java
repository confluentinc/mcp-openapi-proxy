package io.confluent.pas.mcp.proxy.frameworks.java;

import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer;
import io.confluent.pas.mcp.common.services.*;
import io.confluent.pas.mcp.proxy.frameworks.java.kafka.TopicManagement;
import io.confluent.pas.mcp.proxy.frameworks.java.models.Key;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

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
public class SubscriptionHandler<K extends Key, REQ, RES> implements Closeable {

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

    private final KafkaConfiguration kafkaConfigration;
    private final RegistrationService<Schemas.RegistrationKey, Schemas.Registration> registrationService;
    private final TopicManagement topicManagement;
    private final Class<K> keyClass;
    private final Class<REQ> requestClass;
    private final Class<RES> responseClass;
    private KafkaStreams kafkaStreams;
    private final Serdes.WrapperSerde<K> keySerde;
    private final Serdes.WrapperSerde<REQ> reqWrapperSerde;
    private final Serdes.WrapperSerde<RES> resWrapperSerde;

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
        this.kafkaConfigration = kafkaConfigration;
        this.topicManagement = new TopicManagement(kafkaConfigration);
        this.registrationService = new RegistrationService<>(
                kafkaConfigration,
                Schemas.RegistrationKey.class,
                Schemas.Registration.class);

        this.keyClass = keyClass;
        this.requestClass = requestClass;
        this.responseClass = responseClass;

        this.keySerde = getSerdes(keyClass, true);
        this.reqWrapperSerde = getSerdes(requestClass, false);
        this.resWrapperSerde = getSerdes(responseClass, false);
    }

    @Override
    public void close() {
        registrationService.close();
        if (kafkaStreams != null) {
            kafkaStreams.close();
        }
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

        subscribe(registration, handler);
    }

    /**
     * Subscribes to a registration and handles incoming requests.
     *
     * @param registration   Registration to use for the subscription
     * @param handler        RequestHandler to handle the request
     * @param requestSchema  The request schema
     * @param responseSchema The response schema
     * @throws SubscriptionException if there is an error during subscription
     */
    public void subscribeWith(Schemas.Registration registration,
                              JsonSchema requestSchema,
                              JsonSchema responseSchema,
                              RequestHandler<K, REQ, RES> handler) throws SubscriptionException {
        log.info("Subscribing for registration: {}", registration.getName());

        // First we create the topic for the request/response
        try {
            topicManagement.createTopic(registration.getRequestTopicName(), keyClass, requestSchema);
            topicManagement.createTopic(registration.getResponseTopicName(), keyClass, responseSchema);
        } catch (Exception e) {
            log.error("Failed to create topic", e);
            throw new SubscriptionException("Failed to create topic", e);
        }

        subscribe(registration, handler);
    }

    private <VALUE> Serdes.WrapperSerde<VALUE> getSerdes(Class<VALUE> value, boolean isKey) {
        final Map<String, Object> configuration = KafkaPropertiesFactory.getSchemaRegistryConfig(kafkaConfigration, value, isKey);
        final Serdes.WrapperSerde<VALUE> serde = new Serdes.WrapperSerde<>(
                new KafkaJsonSchemaSerializer<>(),
                new KafkaJsonSchemaDeserializer<>());

        serde.configure(configuration, isKey);

        return serde;
    }

    private void subscribe(Schemas.Registration registration, RequestHandler<K, REQ, RES> handler) {
        // Register the capability
        final Schemas.RegistrationKey registrationKey = new Schemas.RegistrationKey(registration.getName());
        if (!registrationService.isRegistered(registrationKey)) {
            log.info("Registering: {}", registration.getName());
            registrationService.register(registrationKey, registration);
        } else {
            log.info("Already registered: {}", registration.getName());
        }


        // Build the KStream topology
        StreamsBuilder builder = new StreamsBuilder();

        builder.stream(registration.getRequestTopicName(), Consumed.with(keySerde, reqWrapperSerde))
                .process(new SubscriptionHandlerSupplier<>(handler, registration))
                .to(registration.getResponseTopicName(), Produced.with(keySerde, resWrapperSerde));

        final Topology topology = builder.build();
        kafkaStreams = new KafkaStreams(topology, KafkaPropertiesFactory.getKStreamsProperties(kafkaConfigration));
        kafkaStreams.start();
    }
}
