package io.confluent.pas.mcp.proxy.registration.internal;

import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientFactory;
import io.confluent.kafka.schemaregistry.json.JsonSchemaProvider;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializerConfig;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer;
import io.confluent.pas.mcp.common.services.KafkaConfigration;
import io.kcache.KafkaCacheConfig;
import lombok.Getter;
import lombok.Setter;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Getter
@Setter
@Component
@ConfigurationProperties("kafka")
public class KafkaConfigrationImpl implements KafkaConfigration {

    private Broker broker;
    private SR schemaRegistry;

    @Bean
    public SR getSchemaRegistryConfiguration() {
        return schemaRegistry;
    }

    /**
     * Get properties for Kafka producer
     *
     * @param appId the application id
     * @return properties
     */
    public Properties getProducerProperties(final String appId) {
        Properties properties = getProperties("");

        properties.put(ProducerConfig.CLIENT_ID_CONFIG, appId);
        properties.put("key.serializer", KafkaJsonSchemaSerializer.class.getName());
        properties.put("value.serializer", KafkaJsonSchemaSerializer.class.getName());

        return properties;
    }

    /**
     * Get properties for Kafka consumer
     *
     * @param appId           the application id
     * @param requireEarliest whether to require earliest offset
     * @param keyType         the key type
     * @param valueType       the value type
     * @return properties
     */
    public Properties getConsumerProperties(final String appId,
                                            final boolean requireEarliest,
                                            Class<?> keyType,
                                            Class<?> valueType) {
        Properties properties = getProperties("");

        properties.put(ConsumerConfig.GROUP_ID_CONFIG, appId + "-group");
        properties.put(ConsumerConfig.CLIENT_ID_CONFIG, appId);
        properties.put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, 1000);

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

    public KafkaCacheConfig getCacheConfig(String appId, String cacheTopic, boolean readOnly) {
        Properties properties = getProperties("kafkacache.");
        properties.put(KafkaCacheConfig.KAFKACACHE_TOPIC_CONFIG, cacheTopic);
        properties.put(KafkaCacheConfig.KAFKACACHE_CLIENT_ID_CONFIG, appId);
        properties.put(KafkaCacheConfig.KAFKACACHE_GROUP_ID_CONFIG, appId + "-group");
        properties.put(KafkaCacheConfig.KAFKACACHE_TOPIC_READ_ONLY_CONFIG, readOnly);
        return new KafkaCacheConfig(properties);
    }

    @Bean
    public SchemaRegistryClient getSchemaRegistryClient() {
        return SchemaRegistryClientFactory.newClient(
                List.of(schemaRegistry.url),
                100,
                List.of(new JsonSchemaProvider(), new AvroSchemaProvider()),
                getSchemaRegistryConfig(),
                new HashMap<>()
        );
    }

    /**
     * Get schema registry configuration
     *
     * @return configuration
     */
    public Map<String, Object> getSchemaRegistryConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("schema.registry.url", schemaRegistry.url);
        config.put("basic.auth.credentials.source", "USER_INFO");
        config.put("schema.registry.basic.auth.user.info", schemaRegistry.auth_info);
        return config;
    }


}
