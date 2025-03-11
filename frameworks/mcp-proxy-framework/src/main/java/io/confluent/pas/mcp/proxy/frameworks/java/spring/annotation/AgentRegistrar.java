package io.confluent.pas.mcp.proxy.frameworks.java.spring.annotation;

import io.confluent.pas.mcp.common.services.KafkaConfiguration;
import io.confluent.pas.mcp.common.services.Schemas;
import io.confluent.pas.mcp.proxy.frameworks.java.SubscriptionHandler;
import io.confluent.pas.mcp.proxy.frameworks.java.models.Key;
import io.confluent.pas.mcp.proxy.frameworks.java.spring.mcp.AsyncMcpToolCallbackProvider;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.context.ApplicationContext;

import java.io.Closeable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Automatically registers and manages agent methods annotated with @Agent in a Spring application.
 * This class acts as a bridge between Spring beans containing agent methods and the Kafka-based messaging system.
 */
@Slf4j
@AutoConfiguration
@AutoConfigureOrder
public class AgentRegistrar implements InitializingBean, Closeable {
    private static final String SELF_BEAN_NAME = AgentRegistrar.class.getSimpleName();

    /**
     * Spring application context for accessing beans
     */
    private final ApplicationContext applicationContext;

    /**
     * Kafka configuration for setting up message handlers
     */
    private final KafkaConfiguration kafkaConfiguration;

    /**
     * List to keep track of all active subscription handlers
     */
    private final List<SubscriptionHandler<?, ?, ?>> handlers = new ArrayList<>();

    /**
     * Creates a new AgentRegistrar with the required dependencies.
     *
     * @param kafkaConfiguration Configuration for Kafka messaging
     * @param applicationContext Spring application context
     */
    public AgentRegistrar(KafkaConfiguration kafkaConfiguration,
                          ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.kafkaConfiguration = kafkaConfiguration;
    }

    /**
     * Initializes the registrar after all properties are set.
     * Scans all Spring beans for methods annotated with @Agent and sets up their handlers.
     */
    @Override
    public void afterPropertiesSet() {
        final String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            // Skip self-registration to avoid infinite loops
            if (beanName.endsWith(SELF_BEAN_NAME)) {
                continue;
            }

            final Object bean = applicationContext.getBean(beanName);

            final Method[] methods = bean.getClass().getMethods();
            Arrays.stream(methods)
                    .filter(m -> m.isAnnotationPresent(Agent.class) || m.isAnnotationPresent(Resource.class))
                    .forEach(method -> {
                        if (method.isAnnotationPresent(Agent.class)) {
                            final Agent agent = method.getAnnotation(Agent.class);
                            final SubscriptionHandler<? extends Key, ?, ?> subscriptionHandler = getSubscriptionHandler(method, agent, bean);
                            // Track the handler for cleanup
                            handlers.add(subscriptionHandler);
                        } else if (method.isAnnotationPresent(Resource.class)) {
                            final Resource resource = method.getAnnotation(Resource.class);
                            final SubscriptionHandler<? extends Key, ?, ?> subscriptionHandler = getSubscriptionHandler(method, resource, bean);
                            // Track the handler for cleanup
                            handlers.add(subscriptionHandler);
                        }
                    });
        }
    }

    /**
     * Creates a subscription handler for the given method and bean.
     *
     * @param method   Method annotated with @Agent
     * @param resource Resource annotation
     * @param bean     Bean containing the method
     * @return Subscription handler for the method
     */
    @NotNull
    private SubscriptionHandler<?, ?, ?> getSubscriptionHandler(Method method, Resource resource, Object bean) {
        log.info("Found resource {} on method {}", resource.name(), method.getName());

        // Create registration info for the agent
        final Schemas.ResourceRegistration registration = new Schemas.ResourceRegistration(
                resource.name(),
                resource.description(),
                resource.request_topic(),
                resource.response_topic(),
                resource.contentType(),
                resource.path());

        // Create and start a subscription handler for the resource
        final SubscriptionHandler<?, ?, ? extends Schemas.ResourceResponse> subscriptionHandler = new SubscriptionHandler<>(
                kafkaConfiguration,
                resource.keyClass(),
                Schemas.ResourceRequest.class,
                resource.responseClass());

        // Set up the message handling by invoking the annotated method
        return subscribe(method, bean, registration, subscriptionHandler);
    }

    /**
     * Creates a subscription handler for the given method and bean.
     *
     * @param method Method annotated with @Agent
     * @param agent  Agent annotation
     * @param bean   Bean containing the method
     * @return Subscription handler for the method
     */
    @NotNull
    private SubscriptionHandler<?, ?, ?> getSubscriptionHandler(Method method, Agent agent, Object bean) {
        log.info("Found agent {} on method {}", agent.name(), method.getName());

        // Create registration info for the agent
        final Schemas.Registration registration = new Schemas.Registration(
                agent.name(),
                agent.description(),
                agent.request_topic(),
                agent.response_topic());

        // Create and start a subscription handler for the agent
        final SubscriptionHandler<?, ?, ?> subscriptionHandler = new SubscriptionHandler<>(
                kafkaConfiguration,
                agent.keyClass(),
                agent.requestClass(),
                agent.responseClass());

        // Set up the message handling by invoking the annotated method
        return subscribe(method, bean, registration, subscriptionHandler);
    }

    /**
     * Subscribes the given method to the subscription handler.
     *
     * @param method              Method to subscribe
     * @param bean                Bean containing the method
     * @param registration        Registration info for the method
     * @param subscriptionHandler Subscription handler to subscribe to
     * @return Subscription handler with the method subscribed
     */
    @NotNull
    private SubscriptionHandler<?, ?, ?> subscribe(Method method,
                                                   Object bean,
                                                   Schemas.Registration registration, SubscriptionHandler<?, ?, ?> subscriptionHandler) {
        subscriptionHandler.subscribeWith(
                registration,
                (request) -> {
                    try {
                        MethodHandles.Lookup lookup = MethodHandles.lookup();
                        MethodHandle methodHandle = lookup.unreflect(method).bindTo(bean);
                        methodHandle.invoke(request);
                    } catch (Throwable e) {
                        log.error("Failed to invoke handler method via MethodHandles", e);
                        throw new AgentInvocationException("Failed to invoke handler method", e);
                    }
                });

        return subscriptionHandler;
    }

    /**
     * Cleans up resources by stopping all subscription handlers.
     */
    @Override
    public void close() {
        handlers.forEach(SubscriptionHandler::close);
        handlers.clear();
    }
}