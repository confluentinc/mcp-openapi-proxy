package io.confluent.pas.mcp.proxy.frameworks.java.spring.annotation;

import io.confluent.pas.mcp.common.services.Schemas;
import io.confluent.pas.mcp.proxy.frameworks.java.models.Key;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Resource {

    String name();

    String description();

    String request_topic();

    String response_topic();

    String contentType();

    String path();

    String correlationId() default "correlationId";

    Class<? extends Key> keyClass() default Key.class;

    Class<? extends Schemas.ResourceResponse> responseClass();

}
