# Java Agent Framework

[Back to Main README](../../README.md)

## Overview

This framework accelerates the development of **MCP/OpenAPI Agents** in Java. It provides built-in support for **agent
registration** and **subscription to request topics**, simplifying agent lifecycle management and interaction with Kafka
topics.

## Features

- **Automatic Agent Registration**: Ensures agents are registered in MCP/OpenAPI.
- **Subscription Handling**: Automatically subscribes agents to request topics and processes responses.
- **Kafka Integration**: Manages Kafka consumers and producers efficiently.
- **Topic Management**: Ensures necessary topics are created dynamically.

## Architecture

```
 +-------------------+        +---------------------------+        +---------------------------+        +-------------------+
 |  Incoming Request | -----> |  MCP/OpenAPI Proxy        | -----> | Java Agent Framework      | -----> | Custom Java Agent |
 |   (MCP Client)    |        |  (Manages Agent Registry) |        |  (Handles Subscription)   |        |(Processes Request)|
 +-------------------+        +---------------------------+        +---------------------------+        +-------------------+
                                                                                   |
                                                                                   v
                                                                   +---------------------------+
                                                                   | Kafka Request/Response    |
                                                                   |  Topics Managed by Agent  |
                                                                   +---------------------------+
```

## Prerequisites

- Java 21+
- Maven
- Confluent Cloud account (Kafka & Schema Registry configured)

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

## Core Class: SubscriptionHandler

The `SubscriptionHandler` class manages agent **registration** and **subscriptions** to request topics.

```java
public class SubscriptionHandler<K extends Key, REQ, RES> {

    public interface RequestHandler<K, REQ, RES> {
        void onRequest(Request<K, REQ, RES> request);
    }

    private final RegistrationService<RegistrationKey, Registration> registrationService;
    private final ProducerService<K, RES> responseService;
    private final ConsumerService<K, REQ> requestService;
    private final TopicManagement topicManagement;
    private final Class<K> keyClass;
    private final Class<REQ> requestClass;
    private final Class<RES> responseClass;

    public SubscriptionHandler(String applicationId,
                               TopicManagement topicManagement,
                               KafkaConfigration kafkaConfigration,
                               String registrationTopic,
                               Class<K> keyClass,
                               Class<REQ> requestClass,
                               Class<RES> responseClass) {
        this.topicManagement = topicManagement;
        this.responseService = new ProducerService<>(applicationId, kafkaConfigration);
        this.requestService = new ConsumerService<>(applicationId, kafkaConfigration, keyClass, requestClass);
        this.registrationService = new RegistrationService<>(
                applicationId, kafkaConfigration, RegistrationKey.class, Registration.class, registrationTopic, false);
        this.keyClass = keyClass;
        this.requestClass = requestClass;
        this.responseClass = responseClass;
    }

    public void start() {
        registrationService.start();
    }

    public void stop() {
        requestService.stop();
    }

    public void subscribeWith(Registration registration, RequestHandler<K, REQ, RES> handler) throws SubscriptionException {
        log.info("Subscribing for registration: {}", registration.getName());

        final RegistrationKey registrationKey = new RegistrationKey(registration.getName());
        if (!registrationService.isRegistered(registrationKey)) {
            try {
                topicManagement.createTopic(registration.getRequestTopicName(), keyClass, requestClass);
                topicManagement.createTopic(registration.getResponseTopicName(), keyClass, responseClass);
            } catch (Exception e) {
                log.error("Failed to create topic", e);
                throw new SubscriptionException("Failed to create topic", e);
            }
            log.info("Registering: {}", registration.getName());
            registrationService.register(registrationKey, registration);
        } else {
            log.info("Already registered: {}", registration.getName());
        }

        requestService.subscribeForEvent(
                registration.getRequestTopicName(),
                (topic, key, request) -> {
                    final Request<K, REQ, RES> requestWrapper = new Request<>(key, request, registration, responseService);
                    handler.onRequest(requestWrapper);
                });
    }
}
```

## Registering an Agent

Agents are automatically registered in MCP/OpenAPI when calling:

```java
subscriptionHandler.subscribeWith(registration, this::onRequest);
```

## Conclusion

This framework streamlines the development of **MCP/OpenAPI Agents** in Java, handling **registration**, **subscriptions
**, and **Kafka topic management**. By leveraging this framework, developers can focus on their agent’s business logic
without worrying about infrastructure setup.

## Contributing

Contributions are welcome! Feel free to open issues and submit pull requests.

## License

This project is licensed under the MIT License.
