package io.confluent.pas.mcp.proxy.registration;

import com.fasterxml.jackson.databind.JsonNode;
import io.confluent.pas.mcp.common.services.KafkaConfiguration;
import io.confluent.pas.mcp.proxy.registration.kafka.ProducerService;
import io.confluent.pas.mcp.common.services.Schemas;
import io.confluent.pas.mcp.proxy.registration.kafka.ConsumerService;
import io.confluent.pas.mcp.proxy.registration.schemas.RegistrationSchemas;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Handle requests and responses
 */
@Slf4j
@Component
public class RequestResponseHandler implements DisposableBean {

    private final ProducerService producerService;
    private final ConsumerService consumerService;

    public RequestResponseHandler(@Autowired KafkaConfiguration kafkaConfiguration) {
        this.consumerService = new ConsumerService(kafkaConfiguration);
        this.producerService = new ProducerService(kafkaConfiguration);
    }

    public void addRegistrations(Collection<Schemas.Registration> registrations) {
        consumerService.addRegistrations(registrations);
    }

    /**
     * Send a request to a topic and wait for a response
     *
     * @param registration  the registration
     * @param schemas       the schemas
     * @param correlationId the correlation id
     * @param request       the request
     * @return the response
     * @throws ExecutionException   if the request fails
     * @throws InterruptedException if the request is interrupted
     */
    public Mono<JsonNode> sendRequestResponse(Schemas.Registration registration,
                                              RegistrationSchemas schemas,
                                              String correlationId,
                                              Map<String, Object> request)
            throws ExecutionException, InterruptedException {
        Sinks.One<JsonNode> sink = Sinks.one();

        // Register the response handler
        consumerService.registerResponseHandler(registration, correlationId, sink::tryEmitValue);

        // Create the Key
        final Map<String, Object> key = Map.of(registration.getCorrelationIdFieldName(), correlationId);

        // Send the request
        return producerService.send(registration.getRequestTopicName(),
                        schemas.getRequestKeySchema().envelope(key),
                        schemas.getRequestSchema().envelope(request))
                .doOnError(sink::tryEmitError)
                .doOnSuccess(metadata -> log.info("Sent request to topic: {}", registration.getRequestTopicName()))
                .then(sink.asMono());
    }

    @Override
    public void destroy() throws Exception {
        consumerService.close();
        producerService.close();
    }
}
