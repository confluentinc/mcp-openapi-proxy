package io.confluent.pas.mcp.proxy.frameworks.java.kafka;

public class TopicManagementException extends RuntimeException {
    public TopicManagementException(String message) {
        super(message);
    }

    public TopicManagementException(String message, Throwable cause) {
        super(message, cause);
    }
}
