package io.confluent.pas.mcp.proxy.registration;

import com.fasterxml.jackson.databind.JsonNode;
import io.confluent.pas.mcp.common.services.ProducerService;
import io.confluent.pas.mcp.common.utils.Lazy;
import io.confluent.pas.mcp.proxy.registration.internal.KafkaConfigrationImpl;
import io.confluent.pas.mcp.proxy.registration.internal.KafkaResponseHandler;
import io.confluent.pas.mcp.proxy.registration.models.Registration;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Handle requests and responses
 */
@Slf4j
@Component
public class RequestResponseHandler {

    private final KafkaConfigrationImpl kafkaConfigration;
    private final ProducerService<JsonNode, JsonNode> producerService;
    private final Lazy<KafkaProducer<JsonNode, JsonNode>> producer = new Lazy<>(this::createNewProducer);
    private final KafkaResponseHandler kafkaResponseHandler;

    public RequestResponseHandler(@Autowired KafkaConfigrationImpl kafkaConfigration) {
        this.kafkaConfigration = kafkaConfigration;
        this.kafkaResponseHandler = new KafkaResponseHandler("mcp-proxy-response", kafkaConfigration);
        this.producerService = new ProducerService<>("mcp-proxy-request", kafkaConfigration);
    }

    /**
     * Send a request to a topic and wait for a response
     *
     * @param registration  the registration
     * @param correlationId the correlation id
     * @param request       the request
     * @return the response
     * @throws ExecutionException   if the request fails
     * @throws InterruptedException if the request is interrupted
     */
    public Mono<Map<String, Object>> sendRequestResponse(Registration registration,
                                                         String correlationId,
                                                         Map<String, Object> request)
            throws ExecutionException, InterruptedException {
        Sinks.One<Map<String, Object>> sink = Sinks.one();

        // Register the response handler
        kafkaResponseHandler.registerResponseHandler(registration, correlationId, sink::tryEmitValue);

        // Create the Key
        final Map<String, Object> key = Map.of(registration.getCorrelationIdFieldName(), correlationId);

        // Send the request
        return producerService.send(registration.getRequestTopicName(),
                        registration.getRequestKeySchema().envelope(key),
                        registration.getRequestSchema().envelope(request))
                .doOnError(sink::tryEmitError)
                .doOnSuccess(metadata -> log.info("Sent request to topic: {}", registration.getRequestTopicName()))
                .then(sink.asMono());
    }

    /**
     * Stop the producer
     */
    @PreDestroy
    public void stop() {
        log.info("Stopping request handler");

        if (producer.isInitialized()) {
            producer.get().close();
        }

        log.info("Request handler stopped");
    }

    /**
     * Create a new producer
     *
     * @return the producer
     */
    private KafkaProducer<JsonNode, JsonNode> createNewProducer() {
        return new KafkaProducer<>(kafkaConfigration.getProducerProperties("mcp-proxy-request"));
    }

}
