package io.confluent.pas.mcp.proxy.frameworks.java;

import io.confluent.pas.mcp.common.services.Schemas;
import io.confluent.pas.mcp.proxy.frameworks.java.models.Key;
import lombok.AllArgsConstructor;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorSupplier;

@AllArgsConstructor
public class SubscriptionHandlerSupplier<K extends Key, REQ, RES> implements ProcessorSupplier<K, REQ, K, RES> {

    private final SubscriptionHandler.RequestHandler<K, REQ, RES> subscriptionHandler;
    private final Schemas.Registration registration;

    @Override
    public Processor<K, REQ, K, RES> get() {
        return new SubscriptionHandlerProcessor<>(subscriptionHandler, registration);
    }
}
