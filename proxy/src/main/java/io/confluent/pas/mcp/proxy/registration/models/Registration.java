package io.confluent.pas.mcp.proxy.registration.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.confluent.kafka.schemaregistry.annotations.Schema;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.pas.mcp.common.services.models.AbstractRegistration;
import io.confluent.pas.mcp.proxy.registration.RegistrationSchema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.IOException;

/**
 * Registration holder, holds the registration and the schemas
 */
@Schema(value = """
        {
           "properties":{
              "correlationIdFieldName":{
                 "connect.index":4,
                 "oneOf":[
                    {
                       "type":"null"
                    },
                    {
                       "type":"string"
                    }
                 ]
              },
              "description":{
                 "connect.index":1,
                 "oneOf":[
                    {
                       "type":"null"
                    },
                    {
                       "type":"string"
                    }
                 ]
              },
              "name":{
                 "connect.index":0,
                 "type":"string"
              },
              "requestTopicName":{
                 "connect.index":2,
                 "oneOf":[
                    {
                       "type":"null"
                    },
                    {
                       "type":"string"
                    }
                 ]
              },
              "responseTopicName":{
                 "connect.index":3,
                 "oneOf":[
                    {
                       "type":"null"
                    },
                    {
                       "type":"string"
                    }
                 ]
              }
           },
           "required":[
              "name"
           ],
           "title":"Record",
           "type":"object"
        }""", refs = {})
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
public class Registration extends AbstractRegistration {

    @JsonIgnore
    private RegistrationSchema requestKeySchema;
    @JsonIgnore
    private RegistrationSchema requestSchema;
    @JsonIgnore
    private RegistrationSchema responseKeySchema;
    @JsonIgnore
    private RegistrationSchema responseSchema;

    public Registration(String name,
                        String description,
                        String requestTopicName,
                        String responseTopicName,
                        String correlationIdFieldName) {
        super(name, description, requestTopicName, responseTopicName, correlationIdFieldName);
    }

    /**
     * Update the schema
     *
     * @param schemaRegistryClient The schema registry client
     * @throws RestClientException If the schema cannot be retrieved
     * @throws IOException         If the schema cannot be retrieved
     */
    public void updateSchema(SchemaRegistryClient schemaRegistryClient) throws RestClientException, IOException {
        this.requestKeySchema = getSchema(getRequestTopicName(), true, schemaRegistryClient);
        this.requestSchema = getSchema(getRequestTopicName(), false, schemaRegistryClient);
        this.responseKeySchema = getSchema(getResponseTopicName(), true, schemaRegistryClient);
        this.responseSchema = getSchema(getResponseTopicName(), false, schemaRegistryClient);
    }

    /**
     * Get the schema for a topic
     *
     * @param topicName The topic name
     * @param forKey    Whether the schema is for the key
     * @return The schema
     * @throws RestClientException If the schema cannot be retrieved
     * @throws IOException         If the schema cannot be retrieved
     */
    private static RegistrationSchema getSchema(String topicName,
                                                boolean forKey,
                                                SchemaRegistryClient schemaRegistryClient) throws RestClientException, IOException {
        final String subject = topicName + (forKey ? "-key" : "-value");
        final String schema = schemaRegistryClient.getLatestSchemaMetadata(subject).getSchema();

        return new RegistrationSchema(schema);
    }
}
