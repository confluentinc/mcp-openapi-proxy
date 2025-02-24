package io.confluent.pas.mcp.proxy.frameworks.java.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.confluent.kafka.schemaregistry.annotations.Schema;
import io.confluent.pas.mcp.common.services.models.AbstractRegistration;
import lombok.NoArgsConstructor;

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
@NoArgsConstructor
public class Registration extends AbstractRegistration {

    public Registration(String name, String description, String requestTopicName, String responseTopicName, String correlationIdFieldName) {
        super(name, description, requestTopicName, responseTopicName, correlationIdFieldName);
    }

    public Registration(String name, String description, String requestTopicName, String responseTopicName) {
        super(name, description, requestTopicName, responseTopicName);
    }
}
