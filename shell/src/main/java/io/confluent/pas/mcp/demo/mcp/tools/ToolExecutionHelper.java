package io.confluent.pas.mcp.demo.mcp.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class ToolExecutionHelper {

    private final static ObjectMapper MAPPER = new ObjectMapper();
    private final static TypeReference<HashMap<String, Object>> TYPE_REFERENCE = new TypeReference<>() {
    };
    private static final String EXECUTION_ERROR_MESSAGE = "There was an error executing the tool";

    /**
     * Extracts a response from a CallToolResult message. This may be an error response.
     */
    public static String extractResult(McpSchema.CallToolResult result) throws JsonProcessingException {
        if (result.isError()) {
            return extractError(result.content());
        } else if (!result.content().isEmpty()) {
            return extractSuccessfulResult(result.content());
        } else {
            log.warn("Result contains neither 'result' nor 'error' element: {}", result);
            return EXECUTION_ERROR_MESSAGE;
        }
    }

    private static String extractSuccessfulResult(List<McpSchema.Content> contents) {
        return contents.stream()
                .map(content -> {
                    if (!content.type().equals("text")) {
                        throw new RuntimeException("Unsupported content type: " + content.type());
                    }
                    return ((McpSchema.TextContent) content).text();
                })
                .collect(Collectors.joining("\n"));
    }

    private static String extractError(List<McpSchema.Content> contents) throws JsonProcessingException {
        final McpSchema.TextContent errorContent = (McpSchema.TextContent) contents.stream().filter(content -> content.type().equals("text"))
                .findFirst()
                .orElseThrow();
        final Map<String, Object> err = MAPPER.readValue(errorContent.text(), TYPE_REFERENCE);

        String errorMessage = "";
        if (err.get("message") != null) {
            errorMessage = err.get("message").toString();
        }
        Integer errorCode = null;
        if (err.get("code") != null) {
            errorCode = Integer.parseInt(err.get("code").toString());
        }
        log.warn("Result contains an error: {}, code: {}", errorMessage, errorCode);
        return EXECUTION_ERROR_MESSAGE;
    }
}
