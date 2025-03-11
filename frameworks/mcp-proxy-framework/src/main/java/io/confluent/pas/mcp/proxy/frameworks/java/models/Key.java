package io.confluent.pas.mcp.proxy.frameworks.java.models;

import io.confluent.kafka.schemaregistry.annotations.Schema;

@Schema(value = """
        {
           "properties": {
             "correlationId": {
               "connect.index": 0,
               "type": "string"
             }
           },
           "required": [
             "correlationId"
           ],
           "title": "Record",
           "type": "object"
         }
        """,
        refs = {})
public record Key(String correlationId) {
}
