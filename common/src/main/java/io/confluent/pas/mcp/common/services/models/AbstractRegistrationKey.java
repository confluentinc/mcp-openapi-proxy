package io.confluent.pas.mcp.common.services.models;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.confluent.kafka.schemaregistry.annotations.Schema;
import io.confluent.pas.mcp.common.services.RegistrationKey;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * A key for a registration.
 */
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
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractRegistrationKey implements RegistrationKey {

    @JsonProperty(value = "name", required = true)
    private String name;
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj instanceof RegistrationKey key && StringUtils.equals(name, key.getName());
    }
}
