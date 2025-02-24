# Proxy

[Back to Main README](../README.md)

## Overview

The **Proxy** is a service that implements the **MCP protocol** and **OpenAPI** to expose Confluent Cloud topics as
a structured API. It enables seamless communication between tools and services by providing a well-defined interface for
interacting with Confluent Cloud topics.

## Key Features

- **MCP Protocol Support**: Allows tool discovery and execution via **stdio or HTTP SSE**.
- **OpenAPI Integration**: Provides a RESTful interface exposing Confluent Cloud topics in the same way as MCP but via
  REST.
- **Schema Registry Support**: Ensures structured data exchange via Confluent's Schema Registry.
- **Correlation ID for Requests/Responses**: Supports tracking of queries across the system.
- **REST API with Swagger UI**: Users can interact with the API through a user-friendly Swagger UI available at /swagger-ui/index.html
- **REST API Tool Endpoints**: Tools are accessible via the endpoint /api/{tool name}.


## Registry Schema

The registry is a specific topic used to register available services. It maintains metadata about services, including
their request and response topics, correlation identifiers, and descriptions.

### Schema Definition

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

