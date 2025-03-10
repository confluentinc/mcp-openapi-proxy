package io.confluent.pas.mcp.common.services;

import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientFactory;
import io.confluent.kafka.schemaregistry.json.JsonSchemaProvider;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializerConfig;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializerConfig;
import io.kcache.KafkaCacheConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Factory class for creating Kafka and Schema Registry configuration properties.
 * Provides utilities to configure Kafka producers, consumers, caches, and Schema Registry clients
 * with appropriate serialization, authentication, and connection settings.
 */
public class KafkaPropertiesFactory {

    /**
     * Creates a Schema Registry client with JSON and Avro schema support.
     *
     * @param configration The Kafka configuration containing connection and auth details
     * @return Configured SchemaRegistryClient instance
     */
    public static SchemaRegistryClient getSchemRegistryClient(KafkaConfiguration configration) {
        return SchemaRegistryClientFactory.newClient(
                List.of(configration.schemaRegistryUrl()),
                100,
                List.of(new JsonSchemaProvider(), new AvroSchemaProvider()),
                getSchemaRegistryConfig(configration),
                new HashMap<>());
    }

    /**
     * Creates Schema Registry configuration with authentication settings.
     *
     * @param kafkaConfiguration The Kafka configuration containing Schema Registry details
     * @return Map of Schema Registry configuration properties
     */
    public static Map<String, Object> getSchemaRegistryConfig(KafkaConfiguration kafkaConfiguration) {
        Map<String, Object> config = new HashMap<>();
        config.put("schema.registry.url", kafkaConfiguration.schemaRegistryUrl());
        config.put("basic.auth.credentials.source", "USER_INFO");
        config.put("schema.registry.basic.auth.user.info", kafkaConfiguration.schemaRegistryBasicAuthUserInfo());
        return config;
    }

    /**
     * Creates properties for a Kafka producer with JSON schema serialization.
     *
     * @param configration The Kafka configuration containing connection and auth details
     * @return Properties configured for a Kafka producer
     */
    public static Properties getProducerProperties(KafkaConfiguration configration) {
        Properties properties = getDefaultProperties(configration, "");
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, configration.applicationId());
        properties.put("key.serializer", KafkaJsonSchemaSerializer.class.getName());
        properties.put("value.serializer", KafkaJsonSchemaSerializer.class.getName());
        properties.put(KafkaJsonSchemaSerializerConfig.AUTO_REGISTER_SCHEMAS, false);
        return properties;
    }

    /**
     * Creates properties for a Kafka consumer with JSON schema deserialization.
     * Supports configurable offset reset and type-specific deserialization.
     *
     * @param configration    The Kafka configuration containing connection and auth details
     * @param requireEarliest If true, sets auto.offset.reset to "earliest"
     * @param keyType         Class type for key deserialization (null for byte array)
     * @param valueType       Class type for value deserialization
     * @return Properties configured for a Kafka consumer
     */
    public static Properties getConsumerProperties(KafkaConfiguration configration,
                                                   boolean requireEarliest,
                                                   Class<?> keyType,
                                                   Class<?> valueType) {
        final Properties properties = getDefaultProperties(configration, "");
        properties.put(ConsumerConfig.CLIENT_ID_CONFIG, configration.applicationId());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, configration.applicationId() + "-group");

        if (requireEarliest) {
            properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        }

        if (keyType == null) {
            properties.put("key.deserializer", ByteArrayDeserializer.class.getName());
        } else {
            properties.put("key.deserializer", KafkaJsonSchemaDeserializer.class.getName());
            properties.put(KafkaJsonSchemaDeserializerConfig.JSON_KEY_TYPE, keyType.getName());
        }

        properties.put("value.deserializer", KafkaJsonSchemaDeserializer.class.getName());
        properties.put(KafkaJsonSchemaDeserializerConfig.JSON_VALUE_TYPE, valueType.getName());
        return properties;
    }

    /**
     * Creates configuration for a Kafka cache.
     * Sets up topic, client ID, and group ID with appropriate suffixes.
     *
     * @param configration The Kafka configuration containing connection and auth details
     * @param readOnly     Whether the cache should be read-only
     * @return KafkaCacheConfig configured for the cache
     */
    public static KafkaCacheConfig getCacheConfig(KafkaConfiguration configration, boolean readOnly) {
        Properties properties = getDefaultProperties(configration, "kafkacache.");
        properties.put(KafkaCacheConfig.KAFKACACHE_TOPIC_CONFIG, configration.registrationTopicName());
        properties.put(KafkaCacheConfig.KAFKACACHE_CLIENT_ID_CONFIG, configration.applicationId() + "-registration");
        properties.put(KafkaCacheConfig.KAFKACACHE_GROUP_ID_CONFIG, configration.applicationId() + "-registration" + "-group");
        properties.put(KafkaCacheConfig.KAFKACACHE_TOPIC_READ_ONLY_CONFIG, readOnly);
        return new KafkaCacheConfig(properties);
    }

    /**
     * Creates properties for a Kafka admin client.
     *
     * @param configration The Kafka configuration containing connection and auth details
     * @return Properties configured for a Kafka admin client
     */
    public static Properties getAdminConfig(KafkaConfiguration configration) {
        return getDefaultProperties(configration, "");
    }

    /**
     * Creates base properties with security and Schema Registry settings.
     * Applies the given prefix to all configuration keys.
     *
     * @param configration        The Kafka configuration containing connection and auth details
     * @param configurationSuffix Prefix to add to configuration keys
     * @return Properties with common Kafka settings
     */
    private static Properties getDefaultProperties(KafkaConfiguration configration, String configurationSuffix) {
        Properties properties = new Properties();
        properties.put(configurationSuffix + "bootstrap.servers", configration.brokerServers());
        properties.put(configurationSuffix + "security.protocol", configration.securityProtocol());
        properties.put(configurationSuffix + "sasl.mechanism", configration.saslMechanism());
        properties.put(configurationSuffix + "sasl.jaas.config", configration.saslJaasConfig());
        properties.put(configurationSuffix + "schema.registry.url", configration.schemaRegistryUrl());
        properties.put(configurationSuffix + "schema.registry.basic.auth.user.info", configration.schemaRegistryBasicAuthUserInfo());
        properties.put(configurationSuffix + "basic.auth.credentials.source", "USER_INFO");
        return properties;
    }
}