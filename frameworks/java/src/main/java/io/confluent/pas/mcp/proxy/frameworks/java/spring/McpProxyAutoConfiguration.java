package io.confluent.pas.mcp.proxy.frameworks.java.spring;

import io.confluent.pas.mcp.common.services.KafkaConfiguration;
import io.confluent.pas.mcp.common.services.TopicConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class McpProxyAutoConfiguration {

    @Value("${kafka.broker-servers}")
    private String brokerServers;
    @Value("${kafka.sr-url}")
    private String schemaRegistryUrl;
    @Value("${kafka.application-id}")
    private String applicationId;
    @Value("${kafka.security-protocol:" + KafkaConfiguration.DEFAULT_SECURITY_PROTOCOL + "}")
    private String securityProtocol;
    @Value("${kafka.sasl-mechanism:" + KafkaConfiguration.DEFAULT_SASL_MECHANISM + "}")
    private String saslMechanism;
    @Value("${kafka.jaas-config}")
    private String saslJaasConfig;
    @Value("${kafka.sr-basic-auth}")
    private String schemaRegistryBasicAuthUserInfo;
    @Value("${kafka.registration-topic-name:" + KafkaConfiguration.DEFAULT_REGISTRATION_TOPIC_NAME + "}")
    private String registrationTopicName;
    @Value("${kafka.topic-configuration:#{null}}")
    private TopicConfiguration topicConfiguration;

    public record KafkaConfigurationImpl(String brokerServers, String schemaRegistryUrl, String applicationId,
                                         String securityProtocol, String saslMechanism, String saslJaasConfig,
                                         String schemaRegistryBasicAuthUserInfo, String registrationTopicName,
                                         TopicConfiguration topicConfiguration) implements KafkaConfiguration {
        public KafkaConfigurationImpl(String brokerServers,
                                      String schemaRegistryUrl,
                                      String applicationId,
                                      String securityProtocol,
                                      String saslMechanism,
                                      String saslJaasConfig,
                                      String schemaRegistryBasicAuthUserInfo,
                                      String registrationTopicName,
                                      TopicConfiguration topicConfiguration) {
            this.brokerServers = brokerServers;
            this.schemaRegistryUrl = schemaRegistryUrl;
            this.applicationId = applicationId;
            this.securityProtocol = getValue(securityProtocol, DEFAULT_SECURITY_PROTOCOL);
            this.saslMechanism = getValue(saslMechanism, DEFAULT_SASL_MECHANISM);
            this.saslJaasConfig = saslJaasConfig;
            this.schemaRegistryBasicAuthUserInfo = schemaRegistryBasicAuthUserInfo;
            this.registrationTopicName = getValue(registrationTopicName, DEFAULT_REGISTRATION_TOPIC_NAME);
            this.topicConfiguration = topicConfiguration == null
                    ? new DefaultTopicConfiguration()
                    : topicConfiguration;
        }

        private static String getValue(final String value, final String defaultValue) {
            return StringUtils.isEmpty(value) ? defaultValue : value;
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public KafkaConfiguration getKafkaConfiguration() {
        return new KafkaConfigurationImpl(
                brokerServers,
                schemaRegistryUrl,
                applicationId,
                securityProtocol,
                saslMechanism,
                saslJaasConfig,
                schemaRegistryBasicAuthUserInfo,
                registrationTopicName,
                topicConfiguration
        );
    }

}
