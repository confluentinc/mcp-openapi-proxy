package io.confluent.pas.mcp.proxy.registration.kafka;

/**
 * Consumer Service Handler interface.
 *
 * @param <K> Key type
 * @param <V> Value type
 */
public interface ConsumerServiceHandler<K, V> {

    /**
     * Handles the received message.
     *
     * @param topic Topic name
     * @param key   Message key
     * @param value Message value
     */
    void onMessage(String topic, K key, V value);
}