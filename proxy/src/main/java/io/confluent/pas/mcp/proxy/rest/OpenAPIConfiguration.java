package io.confluent.pas.mcp.proxy.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.pas.mcp.proxy.registration.RegistrationCoordinator;
import io.confluent.pas.mcp.proxy.registration.models.Registration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.http.HttpStatus;

import java.util.*;
import java.util.stream.Collectors;

/**
 * OpenAPI configuration
 */
@Slf4j
@Configuration
public class OpenAPIConfiguration {

    /**
     * JSON schema
     *
     * @param type                 The type
     * @param properties           The properties
     * @param required             The required properties
     * @param additionalProperties Additional properties
     */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    record JsonSchema(
            @JsonProperty("type") String type,
            @JsonProperty("properties") Map<String, Object> properties,
            @JsonProperty("required") List<String> required,
            @JsonProperty("additionalProperties") Boolean additionalProperties) {
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final RegistrationCoordinator registrationCoordinator;

    @Autowired
    public OpenAPIConfiguration(RegistrationCoordinator registrationCoordinator) {
        this.registrationCoordinator = registrationCoordinator;
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Confluent Tool API")
                        .version("1.0")
                        .license(new License().name("Apache 2.0").url("https://confluent.io")));
    }

    @Bean
    public OpenApiCustomizer openApiCustomizer() {
        return openAPI -> openAPI
                .getPaths()
                .putAll(buildPath());
    }

    /**
     * Build the paths for the API
     *
     * @return The paths
     */
    private Map<String, PathItem> buildPath() {
        final List<Registration> registrations = registrationCoordinator.getAllRegistration();
        final Map<String, PathItem> pathItems = new HashMap<>();

        registrations.forEach(registration -> {
            final String path = registration.getName();
            final PathItem pathItem = new PathItem();

            try {
                final Content requestBody = createRequestBody(registration.getRequestSchema().getSchema());
                final ApiResponse response = createApiResponse(registration.getResponseSchema().getSchema());

                Operation operation = new Operation()
                        .summary(path)
                        .operationId(path)
                        .description(registration.getDescription())
                        .requestBody(new RequestBody().content(requestBody))
                        .responses(new ApiResponses().addApiResponse(String.valueOf(HttpStatus.OK.value()), response));

                pathItem.operation(PathItem.HttpMethod.POST, operation);
                pathItems.put("/api/" + path, pathItem);
            } catch (JsonProcessingException e) {
                log.error("Error parsing input schema", e);
            }
        });

        return pathItems;
    }

    /**
     * Create a request body from the request schema
     *
     * @param requestSchema The request schema
     * @return The request body
     * @throws JsonProcessingException If the request schema cannot be parsed
     */
    private Content createRequestBody(String requestSchema) throws JsonProcessingException {
        final JsonSchema requestSchemaProperties = OBJECT_MAPPER.readValue(requestSchema, JsonSchema.class);
        final Content requestBody = new Content();
        requestBody.addMediaType("application/json", new MediaType().schema(getSchema(requestSchemaProperties)));
        return requestBody;
    }

    /**
     * Create an API response from the response schema
     *
     * @param responseSchema The response schema
     * @return The API response
     * @throws JsonProcessingException If the response schema cannot be parsed
     */
    private ApiResponse createApiResponse(String responseSchema) throws JsonProcessingException {
        final JsonSchema responseSchemaProperties = OBJECT_MAPPER.readValue(responseSchema, JsonSchema.class);
        final ApiResponse response = new ApiResponse();
        response.content(new Content().addMediaType("application/json", new MediaType().schema(getSchema(responseSchemaProperties))));
        response.description(HttpStatus.OK.getReasonPhrase());
        return response;
    }

    /**
     * Create a schema from the schema properties
     */
    @SuppressWarnings("unchecked")
    @NotNull
    private static Schema<?> getSchema(JsonSchema schemaProperties) {
        Schema<?> schema = new Schema<>();
        schema.setType(schemaProperties.type);
        if (schemaProperties.required != null) {
            schema.setRequired(schemaProperties.required);
        }

        schema.setProperties(schemaProperties.properties
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            final Map<String, Object> property = (Map<String, Object>) entry.getValue();
                            final Schema<?> propertySchema = new Schema<>();
                            final Set<String> types = getPropertyType(property);
                            if (types == null || types.isEmpty()) {
                                log.warn("Type is empty for property {}", entry.getKey());
                            }

                            propertySchema.types(types);
                            propertySchema.name(entry.getKey());
                            if (property.containsKey("description")) {
                                propertySchema.description((String) property.get("description"));
                            }

                            return propertySchema;
                        })));

        return schema;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> getPropertyType(Map<String, Object> property) {
        if (property.containsKey("type")) {
            return Set.of(property.get("type").toString());
        }

        if (property.containsKey("oneOf")) {
            final List<Map<String, Object>> oneOf = (List<Map<String, Object>>) property.get("oneOf");
            return oneOf.stream()
                    .map(kv -> kv.get("type").toString())
                    .collect(Collectors.toSet());
        }

        return null;
    }
}