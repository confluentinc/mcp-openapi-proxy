package io.confluent.pas.mcp.proxy.frameworks.java;

import io.confluent.pas.mcp.common.services.Schemas;
import io.confluent.pas.mcp.common.services.ProducerService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import reactor.core.publisher.Mono;

/**
 * Request class that holds the key and the request object
 *
 * @param <K>   Key type
 * @param <REQ> Request type
 * @param <RES> Response type
 */
@AllArgsConstructor
public class Request<K, REQ, RES> {
    @Getter
    private final K key;
    @Getter
    private final REQ request;
    private final Schemas.Registration registration;
    private final ProducerService<K, RES> responseService;

    /**
     * Respond to the request
     *
     * @param response Response object
     */
    public Mono<Void> respond(Response<K, RES> response) {
        return responseService.send(
                registration.getResponseTopicName(),
                response.getKey(),
                response.getResponse());
    }

    /**
     * Respond to the request
     *
     * @param response Response object
     */
    public Mono<Void> respond(RES response) {
        return responseService.send(
                registration.getResponseTopicName(),
                key,
                response);
    }
}
