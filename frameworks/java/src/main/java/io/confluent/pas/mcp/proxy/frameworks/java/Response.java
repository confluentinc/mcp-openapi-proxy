package io.confluent.pas.mcp.proxy.frameworks.java;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Response class that holds the key and the response object
 *
 * @param <K>   Key type
 * @param <RES> Response type
 */
@Getter
@AllArgsConstructor
public class Response<K, RES> {
    private final K key;
    private final RES response;
}
