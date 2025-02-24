package io.confluent.pas.mcp.proxy.registration.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.confluent.kafka.schemaregistry.annotations.Schema;
import io.confluent.pas.mcp.common.services.models.AbstractRegistrationKey;

@Schema(value = """
        {
           "properties": {
             "name": {
               "connect.index": 0,
               "type": "string"
             }
           },
           "required": [
             "name"
           ],
           "title": "Record",
           "type": "object"
         }
        """,
        refs = {})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegistrationKey extends AbstractRegistrationKey {

    public RegistrationKey(String name) {
        super(name);
    }

    public RegistrationKey() {
    }
}
