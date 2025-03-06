package io.confluent.pas.mcp.proxy.frameworks.java.spring.mcp;

import io.modelcontextprotocol.spec.McpSchema;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class ToolsChangeEvent extends ApplicationEvent {

    private final List<McpSchema.Tool> tools;

    public ToolsChangeEvent(Object source, List<McpSchema.Tool> tools) {
        super(source);
        this.tools = tools;
    }
}
