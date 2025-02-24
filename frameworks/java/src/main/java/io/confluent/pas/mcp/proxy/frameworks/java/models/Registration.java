package io.confluent.pas.mcp.proxy.frameworks.java.models;

import io.confluent.pas.mcp.common.services.models.AbstractRegistration;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Registration extends AbstractRegistration {

    public Registration(String name, String description, String requestTopicName, String responseTopicName, String correlationIdFieldName) {
        super(name, description, requestTopicName, responseTopicName, correlationIdFieldName);
    }

    public Registration(String name, String description, String requestTopicName, String responseTopicName) {
        super(name, description, requestTopicName, responseTopicName);
    }
}
