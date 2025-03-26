package io.confluent.pas.mcp.proxy;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.kafka.KafkaContainer;


@Disabled
@SpringBootTest
public class TestProxyIT {

    private final KafkaContainer kafkaContainer;

    @Autowired
    public TestProxyIT(KafkaContainer kafkaContainer) {
        this.kafkaContainer = kafkaContainer;
    }


    @Test
    public void testToolRegistration() {

    }
}
