package io.confluent.pas.mcp.proxy.registration.kafka.exceptions;

/**
 * Exception thrown when a timeout occurs while waiting for a response from a
 * Kafka topic.
 */
public class TimeoutException extends Throwable {

    public TimeoutException(String message) {
        super(message);
    }
}
