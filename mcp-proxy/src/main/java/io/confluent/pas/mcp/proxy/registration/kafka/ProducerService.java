package io.confluent.pas.mcp.proxy.registration.kafka;

import io.confluent.pas.mcp.common.services.KafkaConfiguration;
import io.confluent.pas.mcp.common.services.KafkaPropertiesFactory;
import io.confluent.pas.mcp.common.utils.Lazy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import reactor.core.publisher.Mono;

import java.io.Closeable;

/**
 * ProducerService class that handles sending messages to Kafka topics.
 * This class uses a lazy-initialized KafkaProducer to send messages asynchronously.
 *
 * @param <K> the type of the key
 * @param <V> the type of the value
 */
@Slf4j
@AllArgsConstructor
public class ProducerService<K, V> implements Closeable {

    private final KafkaConfiguration kafkaConfiguration;
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
                            log.error("Error sending message to topic: {}", topic, exception);
                            sink.error(exception);
                        } else {
                            sink.success();
                        }
                    });
        });
    }

    /**
     * Create a new KafkaProducer.
     *
     * @return the KafkaProducer
     */
    private KafkaProducer<K, V> createNewProducer() {
        return new KafkaProducer<>(KafkaPropertiesFactory.getProducerProperties(kafkaConfiguration));
    }

    @Override
    public void close() {
        if (producer.isInitialized()) {
            producer.get().close();
        }
    }
}