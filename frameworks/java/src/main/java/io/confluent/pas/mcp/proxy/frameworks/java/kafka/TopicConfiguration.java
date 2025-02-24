package io.confluent.pas.mcp.proxy.frameworks.java.kafka;

import java.util.Map;

public interface TopicConfiguration {

    /**
     * @return Topic creation timeout in milliseconds
     */
    int getTimeout();

    /**
     * @return Number of partitions
     */
    int getPartitions();

    /**
     * @return Replication factor
     */
    int getReplicationFactor();

    /**
     * @return Topic configuration
     */
    Map<String, String> getConfig();

}
