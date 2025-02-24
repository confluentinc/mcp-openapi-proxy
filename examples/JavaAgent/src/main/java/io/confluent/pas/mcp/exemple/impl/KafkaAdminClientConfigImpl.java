package io.confluent.pas.mcp.exemple.impl;

import io.confluent.pas.mcp.common.services.KafkaConfigration;
import io.confluent.pas.mcp.proxy.frameworks.java.kafka.KafkaAdminClientConfig;
import lombok.Getter;

import java.util.Properties;

@Getter
public class KafkaAdminClientConfigImpl implements KafkaAdminClientConfig {

    private final Properties properties;

    public KafkaAdminClientConfigImpl(KafkaConfigration kafkaConfigration) {
        this.properties = kafkaConfigration.getProperties("");
    }

}
