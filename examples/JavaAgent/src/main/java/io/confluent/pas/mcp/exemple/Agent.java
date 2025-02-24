package io.confluent.pas.mcp.exemple;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import io.confluent.pas.mcp.proxy.frameworks.java.models.Key;
import io.confluent.pas.mcp.proxy.frameworks.java.models.Registration;
import io.confluent.pas.mcp.proxy.frameworks.java.Request;
import io.confluent.pas.mcp.proxy.frameworks.java.SubscriptionHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
