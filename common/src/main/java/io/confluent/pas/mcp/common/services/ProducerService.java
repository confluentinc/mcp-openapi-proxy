package io.confluent.pas.mcp.common.services;

import io.confluent.pas.mcp.common.utils.Lazy;
import lombok.AllArgsConstructor;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import reactor.core.publisher.Mono;

/**
 * ProducerService class that handles sending messages to Kafka topics.
 * This class uses a lazy-initialized KafkaProducer to send messages asynchronously.
 *
 * @param <K> the type of the key
 * @param <V> the type of the value
 */
@AllArgsConstructor
public class ProducerService<K, V> {

    private final String applicationId;
    private final KafkaConfigration kafkaConfigration;
    private final Lazy<KafkaProducer<K, V>> producer = new Lazy<>(this::createNewProducer);

    /**
     * Send a message to a topic.
     *
     * @param topic the topic
     * @param key   the key
     * @param value the value
     * @return a Mono that will complete when the message is sent
     */
    public Mono<Void> send(String topic, K key, V value) {
        return Mono.create(sink -> {
            final ProducerRecord<K, V> record = new ProducerRecord<>(topic, key, value);

            producer.get()
                    .send(record, (metadata, exception) -> {
                        if (exception != null) {
                            sink.error(exception);
                        } else {
                            sink.success();
                        }
                    });
            producer.get().flush();
        });
    }

    /**
     * Create a new KafkaProducer.
     *
     * @return the KafkaProducer
     */
    private KafkaProducer<K, V> createNewProducer() {
        return new KafkaProducer<>(kafkaConfigration.getProducerProperties(applicationId));
    }
}