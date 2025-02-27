package io.confluent.pas.mcp.common.services;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializerConfig;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer;
import io.confluent.pas.mcp.common.utils.SchemaUtils;
import io.kcache.KafkaCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * RegistrationService class that manages the registration cache.
 * This class handles the initialization, registration, and retrieval of registrations
 * using Kafka as the underlying storage mechanism.
 *
 * @param <K> the type of registration key
 * @param <R> the type of registration
 */
@Slf4j
public class RegistrationService<K extends Schemas.RegistrationKey, R extends Schemas.Registration> {

    private final KafkaConfigration kafkaConfigration;
    private final Class<K> registrationKeyClass;
    private final Class<R> registrationClass;
    private final String registrationTopic;
    private final boolean readOnly;
    private final RegistrationServiceHandler.Handler<K, R> handler;
    private final String appId;
    private KafkaCache<K, R> registrationCache;

    /**
     * Constructor for RegistrationService with a handler.
     *
     * @param appId                the application ID
     * @param kafkaConfigration    the Kafka configuration
     * @param registrationKeyClass the class type of the registration key
     * @param registrationClass    the class type of the registration
     * @param registrationTopic    the registration topic name
     * @param readOnly             whether the service is read-only
     * @param handler              the handler for processing registration updates
     */
    public RegistrationService(String appId,
                               KafkaConfigration kafkaConfigration,
                               Class<K> registrationKeyClass,
                               Class<R> registrationClass,
                               String registrationTopic,
                               boolean readOnly,
                               RegistrationServiceHandler.Handler<K, R> handler) {
        this.kafkaConfigration = kafkaConfigration;
        this.registrationKeyClass = registrationKeyClass;
        this.registrationClass = registrationClass;
        this.registrationTopic = registrationTopic;
        this.readOnly = readOnly;
        this.handler = handler;
        this.appId = appId;
    }

    /**
     * Constructor for RegistrationService without a handler.
     *
     * @param appId                the application ID
     * @param kafkaConfigration    the Kafka configuration
     * @param registrationKeyClass the class type of the registration key
     * @param registrationClass    the class type of the registration
     * @param registrationTopic    the registration topic name
     * @param readOnly             whether the service is read-only
     */
    public RegistrationService(String appId,
                               KafkaConfigration kafkaConfigration,
                               Class<K> registrationKeyClass,
                               Class<R> registrationClass,
                               String registrationTopic,
                               boolean readOnly) {
        this(appId, kafkaConfigration, registrationKeyClass, registrationClass, registrationTopic, readOnly, null);
    }

    /**
     * Initialize the registration service.
     * Configures the Kafka serializers and deserializers, and initializes the Kafka cache.
     */
    public void start() {
        final Map<String, Object> srConfig = kafkaConfigration.getSchemaRegistryConfig();
        srConfig.put(KafkaJsonSchemaDeserializerConfig.JSON_KEY_TYPE, registrationKeyClass);
        srConfig.put(KafkaJsonSchemaDeserializerConfig.JSON_VALUE_TYPE, registrationClass);

        final Serde<K> keySerdes = new Serdes.WrapperSerde<>(
                new KafkaJsonSchemaSerializer<>(),
                new KafkaJsonSchemaDeserializer<>()
        );
        keySerdes.configure(srConfig, true);

        final Serde<R> valueSerdes = new Serdes.WrapperSerde<>(
                new KafkaJsonSchemaSerializer<>(),
                new KafkaJsonSchemaDeserializer<>()
        );
        valueSerdes.configure(srConfig, false);

        final RegistrationServiceHandler<K, R> serviceHandler = handler != null
                ? new RegistrationServiceHandler<>(handler)
                : null;

        registrationCache = new KafkaCache<>(
                kafkaConfigration.getCacheConfig(appId, registrationTopic, readOnly),
                keySerdes,
                valueSerdes,
                serviceHandler,
                null
        );

        registrationCache.init();

        if (serviceHandler != null && serviceHandler.isShouldCreateSchemas()) {
            try (final SchemaRegistryClient schemaRegistryClient = SchemaUtils.getSchemaRegistryClient(kafkaConfigration)) {
                SchemaUtils.registerSchemaIfMissing(registrationTopic, registrationKeyClass, true, schemaRegistryClient);
                SchemaUtils.registerSchemaIfMissing(registrationTopic, registrationClass, false, schemaRegistryClient);
            } catch (IOException e) {
                log.error("Error creating schemas", e);
            }
        }
    }

    /**
     * Close the registration service.
     * Closes the Kafka cache and handles any IO exceptions.
     */
    public void close() {
        try {
            registrationCache.close();
        } catch (IOException e) {
            log.error("Error closing registration cache", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Get all registrations.
     *
     * @return the list of all registrations
     */
    public List<R> getAllRegistrations() {
        return registrationCache
                .values()
                .stream()
                .toList();
    }

    /**
     * Check if a registration is already registered.
     *
     * @param key the registration key
     * @return true if the registration is already registered, false otherwise
     */
    public boolean isRegistered(K key) {
        return registrationCache.get(key) != null;
    }

    /**
     * Register a new registration.
     *
     * @param key          the registration key
     * @param registration the registration
     */
    public void register(K key, R registration) {
        registrationCache.put(key, registration);
    }

    /**
     * Unregister a registration.
     *
     * @param key the registration key
     */
    public void unregister(K key) {
        registrationCache.remove(key);
    }

}