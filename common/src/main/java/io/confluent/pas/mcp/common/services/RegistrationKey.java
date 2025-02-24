package io.confluent.pas.mcp.common.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.confluent.kafka.schemaregistry.annotations.Schema;
import org.apache.commons.lang3.StringUtils;

/**
 * A key for a registration.
 * This interface defines the structure of a registration key object.
 * It is annotated with a JSON schema to ensure the correct structure and types of the properties.
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
public interface RegistrationKey extends Comparable<RegistrationKey> {

    /**
     * Get the name of the registration key.
     *
     * @return the name
     */
    String getName();

    /**
     * Compare this registration key with another registration key.
     *
     * @param other the other registration key
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object
     */
    @Override
    default int compareTo(RegistrationKey other) {
        return StringUtils.compare(getName(), other.getName());
    }
}