# Proxy

[Back to Main README](../README.md)

## Table of Contents

1. [Overview](#overview)
2. [Key Features](#key-features)
3. [Registry Schema](#registry-schema)
    - [Tools Schema Definition](#tools-schema-definition)
4. [Request/Response Schema](#requestresponse-schema)
    - [Request Schema](#request-schema)
    - [Response Schema](#response-schema)
    - [Resource Schema Definition](#resource-schema-definition)
5. [Request/Response Schema Definitions](#requestresponse-schema-definitions)
    - [Request Schema Definition](#request-schema-definition)
    - [Response Schemas Definition](#response-schemas-definition)
        - [Text Resource Response Schema](#text-resource-response-schema)
        - [Blob Resource Response Schema](#blob-resource-response-schema)
6. [Correlation ID for Request/Response Handling](#correlation-id-for-requestresponse-handling)
7. [Running the Proxy](#running-the-proxy)
    - [Prerequisites](#prerequisites)
    - [Installation](#installation)
    - [Starting the Proxy](#starting-the-proxy)
8. [Configuration](#configuration)
    - [Required Environment Variables](#required-environment-variables)
    - [Optional Environment Variable](#optional-environment-variable)
9. [Contributing](#contributing)
10. [License](#license)

## Overview

The **Proxy** is a service that implements the **MCP protocol** and **OpenAPI** to expose Confluent Cloud topics as
a structured API. It enables seamless communication between tools and services by providing a well-defined interface for
interacting with Confluent Cloud topics.

## Key Features

- **MCP Protocol Support**: Allows **tool** and **resources** discovery and execution via **stdio or HTTP SSE**.
- **OpenAPI Integration**: Provides a RESTful interface exposing Confluent Cloud topics in the same way as MCP but via
  REST.
- **Schema Registry Support**: Ensures structured data exchange via Confluent's Schema Registry.
- **Correlation ID for Requests/Responses**: Supports tracking of queries across the system.
- **REST API with Swagger UI**: Users can interact with the API through a user-friendly Swagger UI available at
  /swagger-ui/index.html
- **REST API Tool Endpoints**: Tools are accessible via the endpoint /api/{tool name}.
- **REST API Resource Endpoints**: Resources are accessible via the endpoint /rcs/{resource uri}.

## Registry Schema

The registry is a specific topic used to register available services. It maintains metadata about services, including
their request and response topics, correlation identifiers, and descriptions.

### Tools Schema Definition

- **correlationIdFieldName** (*string*): Specifies the field name in the payload that contains the correlation ID, if
  applicable. The default value is `correlationId`.
- **description** (*string*, required): A human-readable description of the service.
- **name** (*string*, required): The unique name of the service being registered.
- **requestTopicName** (*string*, required): The Kafka topic where the service listens for requests.
- **responseTopicName** (*string*, required): The Kafka topic where the service publishes responses.

```json
{
  "properties": {
    "correlationIdFieldName": {
      "type": "string"
    },
    "description": {
      "type": "string"
    },
    "name": {
      "type": "string"
    },
    "requestTopicName": {
      "type": "string"
    },
    "responseTopicName": {
      "type": "string"
    }
  },
  "required": [
    "name",
    "requestTopicName",
    "responseTopicName",
    "description"
  ],
  "title": "Record",
  "type": "object"
}
```

The registry schema is essential for managing tool discovery and execution within MCP. The schema is stored in
Confluent's Schema Registry and follows a structured format to ensure consistency across different tools and services.

## Request/Response Schema

The proxy follows a **schema-first approach**, meaning it relies on Confluent's **Schema Registry** to validate request
and response messages before exposing them through MCP and OpenAPI. Currently, JSON schemas are used for defining and
enforcing data structures.

### Request Schema

Each request follows a structured schema stored in the Schema Registry. A valid request includes a **correlation ID**
for
tracking and a **payload** containing the requested operation.

### Response Schema

Responses must also conform to a registered schema, ensuring consistency and validation. The **correlation ID** must
match the request for proper tracking.

### Resource Schema Definition

- **correlationIdFieldName** (*string*): Specifies the field name in the payload that contains the correlation ID, if
  applicable. The default value is `correlationId`.
- **description** (*string*, required): A human-readable description of the resource.
- **name** (*string*, required): The unique name of the resource being registered.
- **requestTopicName** (*string*, required): The Kafka topic where the resource listens for requests.
- **responseTopicName** (*string*, required): The Kafka topic where the resource publishes responses.
- **mimeType** (*string*, required): The MIME type of the resource.
- **url** (*string*, required): The URL of the resource.

```json
{
  "properties": {
    "registrationType": {
      "connect.index": 5,
      "type": "string"
    },
    "correlationIdFieldName": {
      "connect.index": 4,
      "oneOf": [
        {
          "type": "null"
        },
        {
          "type": "string"
        }
      ]
    },
    "description": {
      "connect.index": 1,
      "type": "string"
    },
    "name": {
      "connect.index": 0,
      "type": "string"
    },
    "requestTopicName": {
      "connect.index": 2,
      "type": "string"
    },
    "responseTopicName": {
      "connect.index": 3,
      "type": "string"
    },
    "mimeType": {
      "connect.index": 6,
      "oneOf": [
        {
          "type": "null"
        },
        {
          "type": "string"
        }
      ]
    },
    "url": {
      "connect.index": 7,
      "oneOf": [
        {
          "type": "null"
        },
        {
          "type": "string"
        }
      ]
    }
  },
  "required": [
    "name",
    "description",
    "registrationType",
    "requestTopicName",
    "responseTopicName"
  ],
  "additionalProperties": false,
  "title": "Record",
  "type": "object"
}
```

The registry schema is essential for managing resource discovery and execution within MCP/OpenAPI. The schema is stored
in
Confluent's Schema Registry and follows a structured format to ensure consistency across different resources and
services.
Resources can contain either text or binary data.

## Request/Response Schema

The proxy follows a **schema-first approach**, meaning it relies on Confluent's **Schema Registry** to validate request
and response messages before exposing them through MCP and OpenAPI. Currently, JSON schemas are used for defining and
enforcing data structures.

### Request Schema Definition

Text and blob resource are supported. The schema is stored in the Schema Registry and follows a structured format to
ensure consistency across different resources types.

```json
{
  "properties": {
    "uri": {
      "connect.index": 0,
      "type": "string"
    }
  },
  "required": [
    "uri"
  ],
  "title": "Record",
  "type": "object"
}
```

### Response Schemas Definition

#### Text Resource Response Schema

Text resource response schema contains a `text` field that can be `null` or a `string`.

```json
{
  "properties": {
    "type": {
      "connect.index": 0,
      "enum": [
        "text",
        "blob"
      ],
      "default": "text",
      "type": "string"
    },
    "uri": {
      "connect.index": 1,
      "type": "string"
    },
    "mimeType": {
      "connect.index": 2,
      "type": "string"
    },
    "text": {
      "connect.index": 3,
      "oneOf": [
        {
          "type": "null"
        },
        {
          "type": "string"
        }
      ]
    }
  },
  "required": [
    "type",
    "uri",
    "mimeType"
  ],
  "title": "Record",
  "type": "object"
}
```

#### Blob Resource Response Schema

Blob resource response schema is similar to text resource response schema, but it contains a `blob` field instead of a
`text` field.
Blobs are base64 encoded binary data.

```json
{
  "properties": {
    "type": {
      "connect.index": 0,
      "enum": [
        "text",
        "blob"
      ],
      "default": "text",
      "type": "string"
    },
    "uri": {
      "connect.index": 1,
      "type": "string"
    },
    "mimeType": {
      "connect.index": 2,
      "type": "string"
    },
    "blob": {
      "connect.index": 3,
      "oneOf": [
        {
          "type": "null"
        },
        {
          "type": "string"
        }
      ]
    }
  },
  "required": [
    "type",
    "uri",
    "mimeType"
  ],
  "title": "Record",
  "type": "object"
}
```

## Correlation ID for Request/Response Handling

To ensure proper tracking of requests and responses, every query must include a **correlation ID**. Instead of being
part of the event payload, the **correlation ID** is derived from the key of the event. This allows efficient message
routing and ensures that responses can be matched correctly with their corresponding requests.

Using the event key as a correlation ID ensures consistency across the system while leveraging Kafka's partitioning and
message ordering mechanisms effectively.

## Running the Proxy

### Prerequisites

- Java 21+
- Maven
- Confluent Cloud account (with Kafka topics and Schema Registry configured)

### Installation

Clone the repository:

```sh
git clone https://...
cd  cd mcp-openapi-proxy/proxy
```

Build the project:

```sh
mvn clean install
```

### Starting the Proxy

```sh
java -jar target/proxy-0.0.1-SNAPSHOT.jar
```

## Configuration

The proxy requires specific environment variables for correct operation:

### Required Environment Variables

- `BROKER_URL` - Confluent Cloud broker URL
- `JAAS_USERNAME` - Authentication username
- `JAAS_PASSWORD` - Authentication password
- `SR_URL` - Schema Registry URL
- `SR_API_KEY` - Schema Registry API key
- `SR_API_SECRET` - Schema Registry API secret

### Optional Environment Variable

- `REGISTRY_TOPIC` - The topic for MCP registry (default: `_agent_registry`)

## Contributing

Contributions are welcome! Feel free to open issues and submit pull requests.

## [License](../LICENSE)

