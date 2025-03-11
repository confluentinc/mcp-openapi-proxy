# MCP Client Proxy

[Back to Main README](../README.md)

A Java-based proxy that exposes MCP (Model Context Protocol) servers as Kafka streaming applications. This tool bridges
the gap between MCP servers and Kafka, allowing agents to discover and utilize tools/resources over Kafka.

## Overview

The MCP Client Proxy is a command-line tool that:

1. Connects to an MCP server (using Standard IO)
2. Exposes the MCP server's functionality as a Kafka streaming application
3. Automatically registers the tool with the MCP/OpenAPI proxy for discovery by agents

This proxy enables seamless integration between MCP-compatible tools and Kafka-based systems, facilitating communication
between various components in your architecture.

## Installation

MCP Client Proxy is a standard Java application built with Maven.

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

### Building from Source

1. Clone the repository:
   ```
   git clone https://github.com/confluentinc/mcp-openapi-proxy.git
   cd mcp-openapi-proxy
   ```

2. Build with Maven:
   ```
   mvn clean package
   ```

3. The build process will generate a JAR file in the `frameworks/mpc-client-proxy/target` directory.

### Using the JAR

You can run the application directly from the JAR file:

```
java -jar ./frameworks/mpc-client-proxy/target/mcp-client-proxy-<version>.jar -c ./cfg.yaml
```

## Usage

```
mcp-client-proxy [options] [command]
```

### Commands

- `help`: Displays usage information and available commands
- `-c, --config <path>`: Specifies the MCP server configuration file

### Options

- `--spring.config.location=<path>`: Override the default proxy configuration file location

### Example Usage

```
mcp-client-proxy -c ./cfg.yaml
```

This will start the proxy using the MCP server configuration file `cfg.yaml` in the current directory.

## Configuration

The MCP Client Proxy has two types of configuration:

1. **Proxy Configuration**: Controls the proxy's behavior, Kafka connectivity, and logging
2. **MCP Server Configuration**: Specifies the MCP server details and tool information

### Proxy Configuration

The proxy itself is configured through:

1. Environment variables (recommended for production/deployment)
2. A configuration file (can be specified with `--spring.config.location`)

#### Standard Proxy Configuration File

```yaml
spring:
  application:
    name: "MCPProxyClient"
  main:
    banner-mode: off
    web-application-type: none
kafka:
  application-id: "${APPLICATION_ID}"
  broker-servers: ${BROKER_URL}
  jaas-config: org.apache.kafka.common.security.plain.PlainLoginModule required username="${JAAS_USERNAME}" password="${JAAS_PASSWORD}";
  sr-url: ${SR_URL}
  sr-basic-auth: ${SR_API_KEY}:${SR_API_SECRET}
logging:
  file:
    name: ${LOG_FILE}
  pattern:
    console: ""
```

#### Environment Variables

The proxy uses the following environment variables:

| Environment Variable | Description                        | Required |
|----------------------|------------------------------------|----------|
| `APPLICATION_ID`     | Kafka application ID               | Yes      |
| `BROKER_URL`         | Kafka broker URL                   | Yes      |
| `JAAS_USERNAME`      | Kafka JAAS authentication username | Yes      |
| `JAAS_PASSWORD`      | Kafka JAAS authentication password | Yes      |
| `SR_URL`             | Schema Registry URL                | Yes      |
| `SR_API_KEY`         | Schema Registry API key            | Yes      |
| `SR_API_SECRET`      | Schema Registry API secret         | Yes      |
| `LOG_FILE`           | Log file path                      | No       |

You can override the default configuration by using the `--spring.config.location` command line argument:

```
mcp-client-proxy --spring.config.location=file:/path/to/custom/application.yml -c ./server-config.yml
```

### MCP Server Configuration

The MCP server details are specified in a separate configuration file that is passed to the proxy with the `-c` or
`--config` option.

#### Configuration File Format

The configuration file uses YAML format with the following structure:

```yaml
# Server execution configuration
command: "<command to execute>"
arguments:
  - "<arg1>"
  - "<arg2>"
  # ...more arguments as needed

# Tool registration information
tool:
  name: "<tool_name>"
  output_schema: "<JSON schema string>"
  # Additional tool properties as needed
```

### Example: Configuration for Python MCP Server

The following example shows how to configure the proxy to connect to a Python-based MCP server:

```yaml
command: "uv"
arguments:
  - "run"
  - "--with"
  - "mcp[cli]"
  - "mcp"
  - "run"
  - "/Users/pascal/projects/oss/mcp-openapi-proxy/examples/python/server.py"
tool:
  name: "add"
  output_schema: "
{
   \"properties\":{
      \"result\":{
         \"connect.index\":0,
         \"type\":\"number\"
      }
   },
   \"required\":[
      \"result\"
   ],
   \"title\":\"Record\",
   \"type\":\"object\"
}
"
```

This configuration:

1. Uses the `uv` command to run a Python MCP server
2. Registers a tool named "add" with the specified output schema
3. Uses stdio mode for communication with the server

## Kafka Integration

The MCP Client Proxy creates the necessary Kafka topics and configurations to expose the MCP server's functionality.
Agents can then discover and utilize these tools through the Kafka streaming interface.

## Tool Registration

The proxy automatically registers the configured tool with the MCP/OpenAPI proxy, enabling discovery by agents. The
registration includes:

- Tool name
- Tool description
- Input/output schemas

## Contributing

Contributions are welcome! Feel free to open issues and submit pull requests.

## [License](LICENSE)