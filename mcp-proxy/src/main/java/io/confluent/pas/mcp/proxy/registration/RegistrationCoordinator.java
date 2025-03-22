package io.confluent.pas.mcp.proxy.registration;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.pas.mcp.common.services.KafkaConfiguration;
import io.confluent.pas.mcp.common.services.KafkaPropertiesFactory;
import io.confluent.pas.mcp.common.services.RegistrationService;
import io.confluent.pas.mcp.common.services.Schemas;
import io.confluent.pas.mcp.proxy.registration.handlers.ResourceHandler;
import io.confluent.pas.mcp.proxy.registration.handlers.ToolHandler;
import io.modelcontextprotocol.server.McpAsyncServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * The registration coordinator is responsible for handling new registrations.
 * It listens for new registrations on the registration topic and processes them.
 */
@Slf4j
@Component
public class RegistrationCoordinator implements DisposableBean {

    private final RequestResponseHandler requestResponseHandler;
    private final McpAsyncServer mcpServer;
    private final Map<String, RegistrationHandler<?, ?>> handlers = new ConcurrentHashMap<>();
    private final SchemaRegistryClient schemaRegistryClient;
    private final RegistrationService<Schemas.RegistrationKey, Schemas.Registration> registrationService;

    @Autowired
    public RegistrationCoordinator(KafkaConfiguration kafkaConfiguration,
                                   RequestResponseHandler requestResponseHandler,
                                   McpAsyncServer mcpServer) {
        this.requestResponseHandler = requestResponseHandler;
        this.mcpServer = mcpServer;
        this.schemaRegistryClient = KafkaPropertiesFactory.getSchemRegistryClient(kafkaConfiguration);
        this.registrationService = new RegistrationService<>(
                kafkaConfiguration,
                Schemas.RegistrationKey.class,
                Schemas.Registration.class,
                this::onRegistration);
    }

    /**
     * Check if a tool is registered
     *
     * @param name The name of the tool
     * @return True if the tool is registered
     */
    public boolean isRegistered(String name) {
        return handlers.containsKey(name);
    }

    /**
     * Get the registration handler for a tool
     *
     * @param name The name of the tool
     * @return The registration handler
     */
    public RegistrationHandler<?, ?> getRegistrationHandler(String name) {
        return handlers.get(name);
    }

    /**
     * Get all registrations
     *
     * @return The registrations
     */
    public List<RegistrationHandler<?, ?>> getAllRegistrationHandlers() {
        return handlers.values().stream().toList();
    }


    /**
     * Register a new tool
     *
     * @param registration The registration
     */
    public void register(Schemas.Registration registration) {
        registrationService.register(new Schemas.RegistrationKey(registration.getName()), registration);
    }

    /**
     * Delete a tool
     *
     * @param name The registration name to delete
     */
    public void unregister(String name) {
        registrationService.unregister(new Schemas.RegistrationKey(name));
    }

    /**
     * Handle a new registration
     *
     * @param registrations The registrations
     */
    private void onRegistration(Map<Schemas.RegistrationKey, Schemas.Registration> registrations) {
        requestResponseHandler.addRegistrations(registrations.values());

        registrations.forEach(this::onRegistration);
    }

    private void onRegistration(Schemas.RegistrationKey key, Schemas.Registration registration) {
        final String registrationName = key.getName();

        // Unregister?
        if (registration == null) {
            // If the registration does not exist, do nothing
            if (!handlers.containsKey(registrationName)) {
                return;
            }

            unregisterHandler(registrationName);
            return;
        }

        if (handlers.containsKey(registrationName)) {
            log.info("Registration already exists, updating: {}", registrationName);
            unregisterHandler(registrationName);
        } else {
            log.info("Received new registration: {}", registrationName);
        }

        try {
            final RegistrationHandler<?, ?> handler = (registration instanceof Schemas.ResourceRegistration rcsRegistration)
                    ? new ResourceHandler(rcsRegistration, schemaRegistryClient, requestResponseHandler)
                    : new ToolHandler(registration, schemaRegistryClient, requestResponseHandler);

            handler.register(mcpServer)
                    .doOnSuccess(v -> {
                        log.info("Added registration: {}", registrationName);
                        handlers.put(registrationName, handler);
                    })
                    .doOnError(e -> {
                        log.error("Error adding registration: {}", registrationName, e);
                        handlers.remove(registrationName);
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error handling registration: {}", registrationName, e);
        }
    }

    /**
     * Unregister a tool
     *
     * @param registrationName The registration name
     */
    private void unregisterHandler(String registrationName) {
        log.info("Unregistering {}", registrationName);

        final RegistrationHandler<?, ?> handler = handlers.get(registrationName);
        if (handler == null) {
            log.warn("No handler with name {}", registrationName);
            return;
        }

        handler.unregister(mcpServer)
                .doOnSuccess(v -> {
                    log.info("Unregistered {}", registrationName);
                    handlers.remove(registrationName);
                })
                .doOnError(e -> log.error("Error unregistering {}", registrationName, e))
                .block();
    }

    @Override
    public void destroy() {
        registrationService.close();
    }
}
