package io.confluent.pas.mcp.proxy.registration.kafka;

import io.confluent.pas.mcp.common.services.KafkaConfiguration;

public class KafkaConsumerConfiguration implements KafkaConfiguration {

    private final String brokerServers;
    private final String schemaRegistryUrl;
    private final String applicationId;
    private final String saslJaasConfig;
    private final String schemaRegistryBasicAuthUserInfo;

    public KafkaConsumerConfiguration(KafkaConfiguration other, String consumerGroup) {
        this.brokerServers = other.brokerServers();
        this.schemaRegistryUrl = other.schemaRegistryUrl();
        this.applicationId = consumerGroup + "_" + other.applicationId();
        this.saslJaasConfig = other.saslJaasConfig();
        this.schemaRegistryBasicAuthUserInfo = other.schemaRegistryBasicAuthUserInfo();
    }

    @Override
    public String brokerServers() {
        return brokerServers;
    }

    @Override
    public String schemaRegistryUrl() {
        return schemaRegistryUrl;
    }

    @Override
    public String applicationId() {
        return applicationId;
    }

    @Override
    public String saslJaasConfig() {
        return saslJaasConfig;
    }

    @Override
    public String schemaRegistryBasicAuthUserInfo() {
        return schemaRegistryBasicAuthUserInfo;
    }
}
