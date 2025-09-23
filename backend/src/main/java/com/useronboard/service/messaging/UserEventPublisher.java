package com.useronboard.service.messaging;

import com.useronboard.service.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * User event publisher for RabbitMQ messaging
 * Publishes user lifecycle events for async processing
 */
@Component
public class UserEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(UserEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.messaging.exchange:user.events}")
    private String exchangeName;

    public UserEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish user registered event
     */
    public void publishUserRegisteredEvent(User user) {
        try {
            Map<String, Object> event = createBaseEvent("user.registered", user);
            event.put("requiresApproval", true);

            rabbitTemplate.convertAndSend(
                exchangeName,
                "user.registered",
                objectMapper.writeValueAsString(event)
            );

            logger.info("Published user registered event for user: {}", user.getId());
        } catch (Exception e) {
            logger.error("Failed to publish user registered event for user: {}", user.getId(), e);
        }
    }

    /**
     * Publish user approved event (triggers welcome email)
     */
    public void publishUserApprovedEvent(User user, String adminId) {
        try {
            Map<String, Object> event = createBaseEvent("user.approved", user);
            event.put("approvedBy", adminId);
            event.put("sendWelcomeEmail", true);

            rabbitTemplate.convertAndSend(
                exchangeName,
                "user.approved",
                objectMapper.writeValueAsString(event)
            );

            logger.info("Published user approved event for user: {} by admin: {}", user.getId(), adminId);
        } catch (Exception e) {
            logger.error("Failed to publish user approved event for user: {}", user.getId(), e);
        }
    }

    /**
     * Publish user rejected event
     */
    public void publishUserRejectedEvent(User user, String adminId, String reason) {
        try {
            Map<String, Object> event = createBaseEvent("user.rejected", user);
            event.put("rejectedBy", adminId);
            event.put("reason", reason);
            event.put("sendNotificationEmail", true);

            rabbitTemplate.convertAndSend(
                exchangeName,
                "user.rejected",
                objectMapper.writeValueAsString(event)
            );

            logger.info("Published user rejected event for user: {} by admin: {}", user.getId(), adminId);
        } catch (Exception e) {
            logger.error("Failed to publish user rejected event for user: {}", user.getId(), e);
        }
    }

    /**
     * Create base event structure
     */
    private Map<String, Object> createBaseEvent(String eventType, User user) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("userId", user.getId());
        event.put("email", user.getEmail());
        event.put("firstName", user.getFirstName());
        event.put("lastName", user.getLastName());
        event.put("status", user.getStatus().toString());
        event.put("timestamp", System.currentTimeMillis());
        return event;
    }
}
