# Java-Based Resource Delivery Agent

[Back to Main README](../../README.md)

## Overview

This sample demonstrates how to build a **Java-based resource provider** that processes incoming requests and delivers
resources via the **MCP/OpenAPI Proxy**. This enables seamless integration with other agents or applications.

## Architecture

```
+-------------------+         +----------------------------+        +------------------+
 |  Incoming Request | -----> |  MCP/OpenAPI Proxy         | -----> | Java Resource    |
 |   (MCP Client)    |        |  (Exposes Java Agent)      |        | Delivery Agent   |
 +-------------------+        +----------------------------+        +------------------+
                                                                        |
                                                                        v
                                                        +----------------------------+
                                                        |  MCP/OpenAPI Proxy         |
                                                        |  (Responds to Query)       |
                                                        +----------------------------+
```

## Prerequisites

- Java 21+
- Maven
- Confluent Cloud account (with Kafka topics and Schema Registry configured)

## Installation

Clone the repository:

```sh
git clone https://...
cd mcp-openapi-proxy
```

Build the project:

```sh
mvn clean install
```

## Running the Agent

Start the Java-based resource delivery agent:

```sh
java -jar ./examples/JavaRCSProvider/target/JavaRCSProvider-0.0.1-SNAPSHOT.jar
```

## Demo Execution with MCP Inspector

To test the agent using the MCP Inspector, follow these steps:

### Step 1: Install and Start MCP Inspector

Run the following command:

```sh
npx mcp-inspector@latest
```

This command will open the MCP Inspector interface in your default web browser.

### Step 2: Connect to MCP Server

In the MCP Inspector interface:

- Enter the MCP server URL (e.g., `http://localhost:8080`).
- Click **Connect**.

### Step 3: List Available Resources

- Go to the **Resources** section in the MCP Inspector UI.
- View the list of available resources, including `JavaResourceAgent`.

### Step 4: Send Requests and Test

- Select the `/client/{client_id}` tool.
- Use the request form to send a sample request, such as:

```
John
```

- Submit the request and view the response directly in the interface.

### Example Output

```json
{
  "contents": [
    {
      "uri": "client/Bob",
      "mimeType": "application/json",
      "text": "{ \"message\": \"Hello, Bob!\" }"
    }
  ]
}
```

### Viewing Logs

- Navigate to the **History** section in the MCP Inspector UI to view real-time logs and responses.

## Conclusion

This sample illustrates how to use MCP Inspector's web interface for testing resource delivery alongside the MCP/OpenAPI
service.
