package com.useronboard.service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for user event processing
 * Defines exchanges, queues, and bindings for async messaging
 */
@Configuration
@EnableRabbit
public class RabbitMQConfig {

    public static final String USER_EVENTS_EXCHANGE = "user.events";

    // Queue names
    public static final String USER_REGISTERED_QUEUE = "user.registered.queue";
    public static final String USER_APPROVED_QUEUE = "user.approved.queue";
    public static final String USER_REJECTED_QUEUE = "user.rejected.queue";

    // Routing keys
    public static final String USER_REGISTERED_ROUTING_KEY = "user.registered";
    public static final String USER_APPROVED_ROUTING_KEY = "user.approved";
    public static final String USER_REJECTED_ROUTING_KEY = "user.rejected";

    /**
     * Topic exchange for user events
     */
    @Bean
    public TopicExchange userEventsExchange() {
        return new TopicExchange(USER_EVENTS_EXCHANGE, true, false);
    }

    /**
     * Queue for user registration events
     */
    @Bean
    public Queue userRegisteredQueue() {
        return QueueBuilder.durable(USER_REGISTERED_QUEUE)
            .withArgument("x-dead-letter-exchange", USER_EVENTS_EXCHANGE + ".dlx")
            .withArgument("x-dead-letter-routing-key", "user.registered.failed")
            .build();
    }

    /**
     * Queue for user approval events
     */
    @Bean
    public Queue userApprovedQueue() {
        return QueueBuilder.durable(USER_APPROVED_QUEUE)
            .withArgument("x-dead-letter-exchange", USER_EVENTS_EXCHANGE + ".dlx")
            .withArgument("x-dead-letter-routing-key", "user.approved.failed")
            .build();
    }

    /**
     * Queue for user rejection events
     */
    @Bean
    public Queue userRejectedQueue() {
        return QueueBuilder.durable(USER_REJECTED_QUEUE)
            .withArgument("x-dead-letter-exchange", USER_EVENTS_EXCHANGE + ".dlx")
            .withArgument("x-dead-letter-routing-key", "user.rejected.failed")
            .build();
    }

    /**
     * Dead letter exchange for failed messages
     */
    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(USER_EVENTS_EXCHANGE + ".dlx", true, false);
    }

    /**
     * Dead letter queue
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(USER_EVENTS_EXCHANGE + ".dlq").build();
    }

    /**
     * Bindings for user events
     */
    @Bean
    public Binding userRegisteredBinding() {
        return BindingBuilder
            .bind(userRegisteredQueue())
            .to(userEventsExchange())
            .with(USER_REGISTERED_ROUTING_KEY);
    }

    @Bean
    public Binding userApprovedBinding() {
        return BindingBuilder
            .bind(userApprovedQueue())
            .to(userEventsExchange())
            .with(USER_APPROVED_ROUTING_KEY);
    }

    @Bean
    public Binding userRejectedBinding() {
        return BindingBuilder
            .bind(userRejectedQueue())
            .to(userEventsExchange())
            .with(USER_REJECTED_ROUTING_KEY);
    }

    /**
     * Dead letter queue binding
     */
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder
            .bind(deadLetterQueue())
            .to(deadLetterExchange())
            .with("*.failed");
    }

    /**
     * JSON message converter
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitMQ template with JSON converter
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
