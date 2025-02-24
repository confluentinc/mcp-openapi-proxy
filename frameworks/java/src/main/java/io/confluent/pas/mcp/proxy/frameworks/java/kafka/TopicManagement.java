package io.confluent.pas.mcp.proxy.frameworks.java.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.victools.jsonschema.generator.*;
import io.confluent.kafka.schemaregistry.annotations.Schema;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.entities.SchemaReference;
import io.confluent.kafka.schemaregistry.client.rest.entities.requests.RegisterSchemaResponse;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.pas.mcp.common.utils.Lazy;
import io.confluent.pas.mcp.common.utils.SchemaUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.errors.TopicExistsException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;


@Slf4j
@AllArgsConstructor
public class TopicManagement {

    private final Lazy<AdminClient> kafkaAdminClient = new Lazy<>(this::getNewKafkaAdminClient);
    private final Lazy<SchemaGenerator> schemaGenerator = new Lazy<>(() -> new SchemaGenerator(new SchemaGeneratorConfigBuilder(
            SchemaVersion.DRAFT_7,
            OptionPreset.PLAIN_JSON).build()));
    private final TopicConfiguration topicConfiguration;
    private final KafkaAdminClientConfig kafkaConfigration;
    private final SchemaRegistryClient schemaRegistryClient;


    /**
     * Create a topic with a schema
     *
     * @param topicName  Topic name
     * @param keyClass   Key class
     * @param valueClass Value class
     * @param <V>        Value type
     * @param <K>        Key type
     * @throws TopicManagementException If the topic cannot be created
     * @throws ExecutionException       If the topic creation fails
     * @throws InterruptedException     If the thread is interrupted
     * @throws TimeoutException         If the topic creation times out
     */
    public <K, V> void createTopic(String topicName, Class<K> keyClass, Class<V> valueClass)
            throws TopicManagementException, ExecutionException, InterruptedException, TimeoutException {
        // First create the topic
        if (!createTopic(topicName)) {
            // Topic already exists, then return
            return;
        }

        // Then register the schemas
        try {
            SchemaUtils.registerSchema(topicName, keyClass, true, schemaRegistryClient);
            SchemaUtils.registerSchema(topicName, valueClass, false, schemaRegistryClient);
        } catch (IOException | RuntimeException | RestClientException e) {
            log.error("Failed to register schema", e);
            throw new TopicManagementException("Failed to register schema", e);
        }
    }

    /**
     * Create a topic
     *
     * @param topic Topic name
     * @throws TopicManagementException If the topic cannot be created
     * @throws InterruptedException     If the thread is interrupted
     * @throws ExecutionException       If the topic creation fails
     * @throws TimeoutException         If the topic creation times out
     *                                  return true if the topic was created, false if it already exists
     */
    private boolean createTopic(String topic)
            throws TopicManagementException, InterruptedException, ExecutionException, TimeoutException {
        final AdminClient admin = kafkaAdminClient.get();

        log.info("Creating topic {}", topic);

        int numLiveBrokers = admin.describeCluster()
                .nodes()
                .get(topicConfiguration.getTimeout(), TimeUnit.MILLISECONDS)
                .size();
        if (numLiveBrokers == 0) {
            throw new TopicManagementException("No live Kafka brokers");
        }

        int topicReplicationFactor = Math.min(numLiveBrokers, topicConfiguration.getReplicationFactor());
        if (topicReplicationFactor < topicConfiguration.getReplicationFactor()) {
            log.warn("Creating the topic {} using a replication factor of {}, which is less than the desired one of {}.",
                    topic,
                    topicReplicationFactor,
                    topicConfiguration.getReplicationFactor());
        }

        Map<String, String> topicConfigs = topicConfiguration.getConfig();
        topicConfigs.put(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT);

        final NewTopic topicRequest = new NewTopic(topic, topicConfiguration.getPartitions(), (short) topicReplicationFactor);
        topicRequest.configs(topicConfigs);

        try {
            admin.createTopics(Collections.singleton(topicRequest))
                    .all()
                    .get(topicConfiguration.getTimeout(), TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof TopicExistsException) {
                log.info("Topic {} already exists", topic);
                return false;
            } else {
                throw e;
            }
        }

        return true;
    }

    private AdminClient getNewKafkaAdminClient() {
        return KafkaAdminClient.create(kafkaConfigration.getProperties());
    }
}