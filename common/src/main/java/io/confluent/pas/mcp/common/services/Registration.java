package io.confluent.pas.mcp.common.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.confluent.kafka.schemaregistry.annotations.Schema;

/**
 * Registration interface that defines the structure of a registration object.
 * This interface is annotated with a JSON schema to ensure the correct structure
 * and types of the properties.
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
public interface Registration {

    /**
     * Get the name of the registration.
     *
     * @return the name
     */
    String getName();

    /**
     * Get the description of the registration.
     *
     * @return the description
     */
    String getDescription();

    /**
     * Get the request topic name.
     *
     * @return the request topic name
     */
    String getRequestTopicName();

    /**
     * Get the response topic name.
     *
     * @return the response topic name
     */
    String getResponseTopicName();

    /**
     * Get the correlation ID field name.
     *
     * @return the correlation ID field name
     */
    String getCorrelationIdFieldName();
}