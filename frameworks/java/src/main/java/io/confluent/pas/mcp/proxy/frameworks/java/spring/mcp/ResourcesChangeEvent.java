package io.confluent.pas.mcp.proxy.frameworks.java.spring.mcp;

import io.modelcontextprotocol.spec.McpSchema;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class ResourcesChangeEvent extends ApplicationEvent {

    private final List<McpSchema.Resource> resources;

    public ResourcesChangeEvent(Object source, List<McpSchema.Resource> tools) {
        super(source);
        this.resources = tools;
    }
}
