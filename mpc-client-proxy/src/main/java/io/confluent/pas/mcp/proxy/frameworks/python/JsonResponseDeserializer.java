package io.confluent.pas.mcp.proxy.frameworks.python;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.confluent.kafka.schemaregistry.json.JsonSchemaUtils;
import io.confluent.pas.mcp.proxy.frameworks.python.exceptions.AgentException;
import io.confluent.pas.mcp.proxy.frameworks.python.models.AgentGenericResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.everit.json.schema.*;


@Slf4j
public class JsonResponseDeserializer {
    private final static ObjectMapper MAPPER = new ObjectMapper();

    private enum JsonType {
        OBJECT,
        STRING,
        INTEGER,
        BOOLEAN,
        DOUBLE
    }

    private final JsonType jsonType;
    private final AgentConfiguration.ToolConfiguration toolConfiguration;

    public JsonResponseDeserializer(AgentConfiguration.ToolConfiguration toolConfiguration) {
        this.toolConfiguration = toolConfiguration;
        this.jsonType = getJsonType();
    }

    public ObjectNode deserialize(String content) {
        try {
            final JsonNode jsonNode;
            switch (jsonType) {
                case JsonType.OBJECT -> {
                    // Check if the content starts with {
                    if (content.charAt(0) != '{') {
                        log.warn("Response content does not start with '{', wrapping in AgentGenericResponse");
                        content = "{\"response\": \"" + StringEscapeUtils.escapeJson(content) + "\"}";
                    }

                    final AgentGenericResponse response = MAPPER.readValue(content, AgentGenericResponse.class);
                    jsonNode = MAPPER.valueToTree(response);
                }
                case JsonType.STRING -> jsonNode = MAPPER.valueToTree(content);
                case JsonType.INTEGER -> {
                    final Integer value = Integer.parseInt(content);
                    jsonNode = MAPPER.valueToTree(value);
                }
                case JsonType.BOOLEAN -> {
                    final Boolean value = Boolean.parseBoolean(content);
                    jsonNode = MAPPER.valueToTree(value);
                }
                case JsonType.DOUBLE -> {
                    final Double value = Double.parseDouble(content);
                    jsonNode = MAPPER.valueToTree(value);
                }
                default -> throw new AgentException("Unsupported JSON type: " + jsonType);
            }

            return JsonSchemaUtils.envelope(toolConfiguration.getOutput_schema(), jsonNode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize JSON: " + content, e);
        }
    }

    private JsonType getJsonType() {
        final Schema schema = toolConfiguration.getOutput_schema().rawSchema();

        if (schema instanceof ObjectSchema) {
            return JsonType.OBJECT;
        } else if (!(schema instanceof ConstSchema) && !(schema instanceof EnumSchema)) {
            if (schema instanceof ArraySchema) {
                return JsonType.OBJECT;
            } else if (schema instanceof CombinedSchema) {
                return JsonType.OBJECT;
            } else if (schema instanceof StringSchema) {
                return JsonType.STRING;
            } else if (schema instanceof NumberSchema numberSchema) {
                return numberSchema.requiresInteger() ? JsonType.INTEGER : JsonType.DOUBLE;
            } else {
                return schema instanceof BooleanSchema ? JsonType.BOOLEAN : JsonType.OBJECT;
            }
        } else {
            return JsonType.OBJECT;
        }
    }

}
