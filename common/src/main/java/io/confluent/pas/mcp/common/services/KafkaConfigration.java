package io.confluent.pas.mcp.common.services;

import io.kcache.KafkaCacheConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * KafkaConfigration interface that provides Kafka configuration properties.
 * This interface is used to configure Kafka producers, consumers, and schema registry.
 */
public interface KafkaConfigration {

    /**
     * Get the broker configuration.
     *
     * @return the broker configuration
     */
    Broker getBroker();

    /**
     * Get the schema registry configuration.
     *
     * @return the schema registry configuration
     */
    SR getSchemaRegistry();

    /**
     * Get the producer properties.
     *
     * @param appId the application ID
     * @return the producer properties
     */
    Properties getProducerProperties(String appId);

    /**
     * Get properties for Kafka consumer.
     *
     * @param appId           the application ID
     * @param requireEarliest whether to require earliest offset
     * @param keyType         the key type
     * @param valueType       the value type
     * @return the consumer properties
     */
    Properties getConsumerProperties(String appId, final boolean requireEarliest, Class<?> keyType, Class<?> valueType);

    /**
     * Get cache configuration.
     *
     * @param appId      the application ID
     * @param cacheTopic the cache topic
     * @param readOnly   whether the cache is read-only
     * @return the cache configuration
     */
    KafkaCacheConfig getCacheConfig(String appId, String cacheTopic, boolean readOnly);

    /**
     * Get the schema registry configuration as a map.
     *
     * @return the schema registry configuration map
     */
    default Map<String, Object> getSchemaRegistryConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("schema.registry.url", getSchemaRegistry().url);
        config.put("basic.auth.credentials.source", "USER_INFO");
        config.put("schema.registry.basic.auth.user.info", getSchemaRegistry().auth_info);
        return config;
    }

    /**
     * Get properties for Kafka producer and consumer.
     *
     * @param configurationSuffix the configuration suffix
     * @return the properties
     */
    default Properties getProperties(String configurationSuffix) {
        Properties properties = new Properties();

        properties.put(configurationSuffix + "bootstrap.servers", getBroker().url);
        properties.put(configurationSuffix + "security.protocol", getBroker().security_protocol);
        properties.put(configurationSuffix + "sasl.mechanism", getBroker().sasl_mechanism);
        properties.put(configurationSuffix + "sasl.jaas.config", getBroker().jaas_config);
        properties.put(configurationSuffix + "schema.registry.url", getSchemaRegistry().url);
        properties.put(configurationSuffix + "schema.registry.basic.auth.user.info", getSchemaRegistry().auth_info);
        properties.put(configurationSuffix + "basic.auth.credentials.source", "USER_INFO");

        return properties;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    class SR {
        public String url;
        public String auth_info;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    class Broker {
        private String url;
        private String security_protocol;
        private String sasl_mechanism;
        private String jaas_config;
    }
}