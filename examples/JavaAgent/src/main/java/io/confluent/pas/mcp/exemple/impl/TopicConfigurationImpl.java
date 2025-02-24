package io.confluent.pas.mcp.exemple.impl;

import io.confluent.pas.mcp.proxy.frameworks.java.kafka.TopicConfiguration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties("topic")
public class TopicConfigurationImpl implements TopicConfiguration {

    private int timeout = 10000;
    private int partitions = 6;
    private int replicationFactor = 3;
    private Map<String, String> config = new HashMap<>();

}
