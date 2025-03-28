package io.confluent.pas.mcp.proxy.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.confluent.pas.mcp.common.services.Schemas;
import io.confluent.pas.mcp.common.utils.JsonUtils;
import io.confluent.pas.mcp.common.utils.UriTemplate;
import io.confluent.pas.mcp.proxy.registration.RegistrationCoordinator;
import io.confluent.pas.mcp.proxy.registration.RegistrationHandler;
import io.confluent.pas.mcp.proxy.registration.schemas.RegistrationSchemas;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.data.Json;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.http.HttpStatus;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Configuration class for OpenAPI.
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

    private final RegistrationCoordinator registrationCoordinator;

    @Autowired
    public OpenAPIConfiguration(RegistrationCoordinator registrationCoordinator) {
        this.registrationCoordinator = registrationCoordinator;
    }

    /**
     * Bean for OpenAPI configuration.
     *
     * @return the OpenAPI instance
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Confluent Tool API")
                        .version("1.0")
                        .license(new License().name("Apache 2.0").url("https://confluent.io")));
    }

    /**
     * Bean for customizing OpenAPI paths.
     *
     * @return the OpenApiCustomizer instance
     */
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
        final List<RegistrationHandler<?, ?>> registrationHandlers = registrationCoordinator.getAllRegistrationHandlers();
        final Map<String, PathItem> pathItems = new HashMap<>();

        registrationHandlers.forEach(handler -> {
            final Schemas.Registration registration = handler.getRegistration();
            final RegistrationSchemas schemas = handler.getSchemas();

            if (registration instanceof Schemas.ResourceRegistration resourceRegistration) {
                addResourcePath(resourceRegistration, registration, pathItems);
                return;
            }

            final String path = registration.getName();
            final PathItem pathItem = new PathItem();

            try {
                final Content requestBody = createRequestBody(schemas.getRequestSchema().getSchema());
                final ApiResponse response = createApiResponse(schemas.getResponseSchema().getSchema());

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
     * Adds a resource path to the path items map.
     *
     * @param resourceRegistration the resource registration
     * @param registration         the registration
     * @param pathItems            the path items map
     */
    private void addResourcePath(Schemas.ResourceRegistration resourceRegistration,
                                 Schemas.Registration registration,
                                 Map<String, PathItem> pathItems) {
        final PathItem pathItem = new PathItem();
        final String path = resourceRegistration.getUrl();

        final ApiResponse response = createApiResponse(resourceRegistration);
        Operation operation = new Operation()
                .summary(path)
                .operationId(path)
                .description(registration.getDescription())
                .responses(new ApiResponses().addApiResponse(String.valueOf(HttpStatus.OK.value()), response));

        pathItem.operation(PathItem.HttpMethod.GET, operation);

        // Add parameter for resource name
        UriTemplate template = new UriTemplate(path);
        template.getParts()
                .stream()
                .filter(part -> part instanceof UriTemplate.TemplatePart)
                .forEach(part -> {
                    UriTemplate.TemplatePart templatePart = (UriTemplate.TemplatePart) part;
                    if (templatePart.getNames() != null) {
                        templatePart.getNames().forEach(name -> {
                            pathItem.addParametersItem(new Parameter()
                                    .name(name)
                                    .in("path")
                                    .required(true)
                                    .schema(new Schema<>().type("string")));
                        });
                    }
                });

        pathItems.put("/rcs/" + path, pathItem);
    }

    /**
     * Creates a request body from the request schema.
     *
     * @param requestSchema the request schema
     * @return the content for the request body
     * @throws JsonProcessingException if the request schema cannot be parsed
     */
    private Content createRequestBody(String requestSchema) throws JsonProcessingException {
        final JsonSchema requestSchemaProperties = JsonUtils.toObject(requestSchema, JsonSchema.class);
        final Content requestBody = new Content();
        requestBody.addMediaType("application/json", new MediaType().schema(getSchema(requestSchemaProperties)));
        return requestBody;
    }

    /**
     * Creates an API response from the response schema.
     *
     * @param registration the resource registration
     * @return the API response
     */
    private ApiResponse createApiResponse(Schemas.ResourceRegistration registration) {
        final Content content = new Content();
        content.addMediaType(registration.getMimeType(), new MediaType());

        final ApiResponse response = new ApiResponse();
        response.content(content);
        response.description(HttpStatus.OK.getReasonPhrase());
        return response;
    }

    /**
     * Create an API response from the response schema
     *
     * @param responseSchema The response schema
     * @return The API response
     * @throws JsonProcessingException If the response schema cannot be parsed
     */
    private ApiResponse createApiResponse(String responseSchema) throws JsonProcessingException {
        final JsonSchema responseSchemaProperties = JsonUtils.toObject(responseSchema, JsonSchema.class);
        final ApiResponse response = new ApiResponse();
        response.content(new Content().addMediaType("application/json", new MediaType().schema(getSchema(responseSchemaProperties))));
        response.description(HttpStatus.OK.getReasonPhrase());
        return response;
    }

    /**
     * Creates a schema from the schema properties.
     *
     * @param schemaProperties the schema properties
     * @return the schema
     */
    @SuppressWarnings("unchecked")
    @NotNull
    private static Schema<?> getSchema(JsonSchema schemaProperties) {
        Schema<?> schema = new Schema<>();
        schema.setType(schemaProperties.type);
        if (schemaProperties.required != null) {
            schema.setRequired(schemaProperties.required);
        }

        //Adding support for primitive types
        switch (schemaProperties.type) {
            case "string" -> schema.setType("string");
            case "integer" -> schema.setType("integer");
            case "boolean" -> schema.setType("boolean");
            case "array" -> schema.setType("array");
            default -> schema.setProperties(schemaProperties.properties
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
        }
        
        if (schemaProperties.additionalProperties != null) {
            schema.setAdditionalProperties(schemaProperties.additionalProperties);
        }

        return schema;
    }

    /**
     * Gets the property type from the property map.
     *
     * @param property the property map
     * @return a set of property types
     */
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