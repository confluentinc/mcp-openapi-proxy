package io.confluent.pas.mcp.common.services.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.confluent.kafka.schemaregistry.annotations.Schema;
import io.confluent.pas.mcp.common.services.Registration;
import lombok.*;

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
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
public abstract class AbstractRegistration implements Registration {
    @JsonProperty(value = "name", required = true)
    private String name;
    @JsonProperty(value = "description", required = true)
    private String description;
    @JsonProperty(value = "requestTopicName", required = true)
    private String requestTopicName;
    @JsonProperty(value = "responseTopicName", required = true)
    private String responseTopicName;
    @JsonProperty(value = "correlationIdFieldName", defaultValue = "correlationId")
    private String correlationIdFieldName;

    protected AbstractRegistration(String name,
                                   String description,
                                   String requestTopicName,
                                   String responseTopicName) {
        this(name, description, requestTopicName, responseTopicName, "correlationId");
    }
}
