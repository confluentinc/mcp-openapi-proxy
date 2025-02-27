# Java-Based Sentiment Analysis Agent

[Back to Main README](../../README.md)

## Overview

This sample demonstrates how to build a **Java-based sentiment analysis agent** that processes incoming requests,
performs sentiment classification, and exposes the results via the **MCP/OpenAPI Proxy**. This enables seamless
integration with other agents or applications.

## Architecture

```
 +-------------------+        +----------------------------+        +-------------------+
 |  Incoming Request | -----> |  MCP/OpenAPI Proxy         | -----> | Java Sentiment    |
 |   (MCP Client)    |        |  (Exposes Java Agent)      |        | Analysis Agent    |
 +-------------------+        +----------------------------+        +-------------------+
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

Start the Java-based sentiment analysis agent:

```sh
java -jar ./exemples/JavaAgent/target/JavaAgent-0.0.1-SNAPSHOT.jar
```

## Configuration

The agent requires the following environment variables:

### Required Environment Variables

- `BROKER_URL` - Confluent Cloud broker URL
- `JAAS_USERNAME` - Authentication username
- `JAAS_PASSWORD` - Authentication password
- `SR_URL` - Schema Registry URL
- `SR_API_KEY` - Schema Registry API key
- `SR_API_SECRET` - Schema Registry API secret
- `GEMINI_API_KEY` - API key for accessing Gemini model

## Java Implementation

### Core Agent Implementation

The main Java class that processes incoming sentiment analysis requests and integrates with the Gemini model:

```java

/**
 * Agent class responsible for handling sentiment analysis requests.
 */
@Slf4j
@Component
public class Agent {

    @Value("${model.gemini-key}")
    private String geminiKey;

    @Value("${model.system-prompt}")
    private String systemPrompt;

    private final SubscriptionHandler<Key, AgentQuery, AgentResponse> subscriptionHandler;
    private final Registration registration;
    private Assistant assistant;

    @Autowired
    public Agent(SubscriptionHandler<Key, AgentQuery, AgentResponse> subscriptionHandler,
                 Registration registration) {
        this.subscriptionHandler = subscriptionHandler;
        this.registration = registration;
    }

    /**
     * Initializes the agent by starting the subscription handler and setting up the assistant.
     */
    @PostConstruct
    public void init() {
        // Start the subscription handler
        subscriptionHandler.start();

        // Subscribe using the registration information and handle requests with the onRequest method
        subscriptionHandler.subscribeWith(registration, this::onRequest);

        // Create a ChatLanguageModel using the Google AI Gemini model
        final ChatLanguageModel chatLanguageModel = GoogleAiGeminiChatModel.builder()
                .apiKey(geminiKey)
                .modelName("gemini-2.0-flash")
                .build();

        // Build the assistant using the AI services
        assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(chatLanguageModel)
                .systemMessageProvider((val) -> systemPrompt)
                .build();
    }

    /**
     * Cleans up resources by stopping the subscription handler.
     */
    @PreDestroy
    public void destroy() {
        // Stop the subscription handler
        subscriptionHandler.stop();
    }

    /**
     * Handles incoming requests by processing the query and responding with the sentiment analysis result.
     *
     * @param request The incoming request containing the query.
     */
    private void onRequest(Request<Key, AgentQuery, AgentResponse> request) {
        log.info("Received request: {}", request.getRequest().query());

        // Process the query using the assistant and get the response
        final String response = assistant.chat(request.getRequest().query());

        // Respond to the request with the sentiment analysis result
        request.respond(new AgentResponse(response))
                .doOnError(e -> log.error("Failed to respond", e))
                .block();
    }
}

```

The agent is registered when calling the `subscriptionHandler.subscribe(registration, this::onRequest);` method. The *
*registration** is injected using Spring Boot's autowiring capability, and it defines the agent's metadata and
associated topics.

#### Registration Bean Definition

```java

@Bean
public Registration registration() {
    return new Registration(
            applicationId,
            "This agent returns sentiments of a human request.",
            requestTopic,
            responseTopic);
}

```

## Demo Execution

To test the agent, follow these steps:

### Step 1: Start the MCP/OpenAPI Proxy

```sh
java -jar proxy/target/proxy-0.0.1-SNAPSHOT.jar
```

### Step 2: Start the Java Sentiment Agent

```sh
java -jar exemples/JavaAgent/target/JavaAgent-0.0.1-SNAPSHOT.jar
```

### Step 3: Send Sample Requests

Send sentiment requests via the MCP shell:

```sh
mcp add sse http://localhost:8080
mcp list tools "MCP Server"
llm gemini
```

### Example Interaction in MCP Shell

```sh
shell:> mcp list tools "Confluent MCP Proxy"
--------------------
Tools for server: Confluent MCP Proxy
....................
Name: JavaSentimentAgent
Description: This agent analyzes sentiment in human queries.
--------------------
shell:> llm gemini
Starting conversation with Gemini model...
Conversation started
User: Hello, I am feeling happy today!
Assistant: Your sentiment is Positive.
User: exit
Conversation ended
shell:> exit
```

## Conclusion

This sample illustrates how a **Java-based Agent** can be exposed as an **MCP/OpenAPI service**, enabling seamless
integration with **MCP/OpenAPI clients**. By leveraging the **MCP/OpenAPI Proxy**, real-time sentiment analysis can be
efficiently integrated into a broader ecosystem.

## Contributing

Contributions are welcome! Feel free to open issues and submit pull requests.

## License

This project is licensed under the MIT License.
