package io.confluent.pas.mcp.proxy.registration.schemas;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.pas.mcp.common.services.Schemas;
import lombok.Getter;

import java.io.IOException;

@Getter
public class RegistrationSchemas {

    private final RegistrationSchema requestKeySchema;
    private final RegistrationSchema requestSchema;
    private final RegistrationSchema responseKeySchema;
    private final RegistrationSchema responseSchema;

    public RegistrationSchemas(SchemaRegistryClient client,
                               Schemas.Registration registration) throws IOException, RestClientException {
        this.requestKeySchema = getSchema(registration.getRequestTopicName(), true, client);
        this.requestSchema = getSchema(registration.getRequestTopicName(), false, client);
        this.responseKeySchema = getSchema(registration.getResponseTopicName(), true, client);
        this.responseSchema = getSchema(registration.getResponseTopicName(), false, client);
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
