package io.confluent.pas.mcp.common.services;

import io.confluent.pas.mcp.common.utils.AutoReadWriteLock;
import io.confluent.pas.mcp.common.utils.Lazy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ConsumerService class that handles requests from Kafka topics.
 * This class manages Kafka consumers, subscribes to topics, and processes incoming messages.
 *
 * @param <K> Key type
 * @param <V> Value type
 */
@Slf4j
public class ConsumerService<K, V> {

    private final KafkaConfiguration kafkaConfiguration;
    private final Class<K> keyClass;
    private final Class<V> requestClass;

    private final ConcurrentHashMap<String, ConsumerServiceHandler<K, V>> subscriptions = new ConcurrentHashMap<>();
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final AtomicBoolean shouldConsumerSubscribe = new AtomicBoolean(false);
    private final AutoReadWriteLock lock = new AutoReadWriteLock();
    private final Lazy<KafkaConsumer<K, V>> consumer = new Lazy<>(this::getNewConsumer);
    private final ExecutorService executorSvc = Executors.newSingleThreadExecutor();

    /**
     * Constructor for ConsumerService.
     *
     * @param kafkaConfiguration The Kafka configuration
     * @param keyClass           The class type of the key
     * @param requestClass       The class type of the request
     */
    public ConsumerService(KafkaConfiguration kafkaConfiguration,
                           Class<K> keyClass,
                           Class<V> requestClass) {
        this.kafkaConfiguration = kafkaConfiguration;
        this.keyClass = keyClass;
        this.requestClass = requestClass;

        // Start the consumer loop
        stopRequested.set(false);
        executorSvc.submit(this::runLoop);
    }

    /**
     * Add a subscription to a topic.
     *
     * @param topic   The topic to subscribe to
     * @param handler Subscription handler
     * @throws IllegalArgumentException if the subscription already exists
     */
    public void subscribeForEvent(String topic, ConsumerServiceHandler<K, V> handler) throws IllegalArgumentException {
        if (subscriptions.containsKey(topic)) {
            return;
        }

        lock.writeLockAndExecute(() -> {
            subscriptions.put(topic, handler);
            shouldConsumerSubscribe.set(true);
        });
    }

    /**
     * Remove a subscription from a topic.
     *
     * @param topic The topic to unsubscribe from
     */
    public void unsubscribeForEvent(String topic) {
        lock.writeLockAndExecute(() -> {
            subscriptions.remove(topic);
            shouldConsumerSubscribe.set(true);
        });
    }

    /**
     * Stop the service.
     */
    public void stop() {
        stopRequested.set(true);
        executorSvc.shutdown();
        try {
            executorSvc.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Error stopping executor", e);
        }

        if (consumer.isInitialized()) {
            consumer.get().close();
        }

        log.info("Stopped RequestService");
    }

    /**
     * Topic subscription and consumer polling loop.
     */
    private void runLoop() {
        log.info("Starting RequestService loop");

        final KafkaConsumer<K, V> consumer = this.consumer.get();

        while (!stopRequested.get()) {
            if (!subscribeToTopics(consumer)) {
                sleep();
                continue;
            }

            consumer.poll(Duration.ofMillis(100))
                    .forEach(record -> {
                        final String topic = record.topic();

                        lock.readLockAndExecute(() -> {
                            ConsumerServiceHandler<K, V> handler = subscriptions.get(topic);
                            if (handler != null) {
                                try {
                                    handler.onMessage(record.topic(), record.key(), record.value());
                                } catch (Exception e) {
                                    log.error("Failed to process message", e);
                                }
                            }
                        });
                    });
        }
    }

    /**
     * Subscribe to topics.
     *
     * @param consumer KafkaConsumer
     * @return true if subscribed to topics
     */
    private boolean subscribeToTopics(KafkaConsumer<K, V> consumer) {
        try {
            return lock.readLockAndExecute(() -> {
                if (!shouldConsumerSubscribe.get()) {
                    return !consumer.subscription().isEmpty();
                }

                shouldConsumerSubscribe.set(false);
                if (!consumer.subscription().isEmpty()) {
                    // Unsubscribe from all topics
                    consumer.unsubscribe();
                }

                if (subscriptions.isEmpty()) {
                    log.info("No subscriptions");
                    return false;
                }

                consumer.subscribe(subscriptions.keySet());
                log.info("Subscribed to topics.");
                return true;
            });
        } catch (Exception e) {
            log.error("Error subscribing to topics", e);
            return false;
        }
    }

    /**
     * Sleep for a second.
     */
    private void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.error("Error sleeping", e);
        }
    }

    /**
     * Create a new KafkaConsumer.
     *
     * @return the KafkaConsumer
     */
    private KafkaConsumer<K, V> getNewConsumer() {
        return new KafkaConsumer<>(KafkaPropertiesFactory.getConsumerProperties(
                kafkaConfiguration,
                false,
                keyClass,
                requestClass));
    }
}