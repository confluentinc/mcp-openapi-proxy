package io.confluent.pas.mcp.proxy.rest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

@Configuration(proxyBeanMethods = false)
public class ToolRestConfiguration {

    @Bean
    public RouterFunction<ServerResponse> route(ToolRestController toolRestController) {
        return RouterFunctions.route(
                POST("/api/{toolName}").and(accept(MediaType.APPLICATION_JSON)),
                toolRestController::processRequest
        );
    }

}
