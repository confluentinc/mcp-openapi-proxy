package io.confluent.pas.mcp.common.services;

import java.util.Map;

public interface TopicConfiguration {

    /**
     * @return Topic creation timeout in milliseconds
     */
    default int getTimeout() {
        return 10000;
    }

    /**
     * @return Number of partitions
     */
    default int getPartitions() {
        return 6;
    }

    /**
     * @return Replication factor
     */
    default int getReplicationFactor() {
        return 3;
    }

    /**
     * @return Topic configuration
     */
    default Map<String, String> getConfig() {
        return null;
    }

}
