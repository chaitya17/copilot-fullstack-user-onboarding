package com.useronboard.service.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * User event consumer for processing async operations
 * Handles welcome emails and notifications with resilience patterns
 */
@Component
public class UserEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(UserEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final EmailService emailService;

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    public UserEventConsumer(ObjectMapper objectMapper, EmailService emailService) {
        this.objectMapper = objectMapper;
        this.emailService = emailService;
    }

    /**
     * Handle user approved events - send welcome email
     */
    @RabbitListener(queues = "user.approved.queue")
    @CircuitBreaker(name = "emailService", fallbackMethod = "handleApprovedEventFallback")
    public void handleUserApprovedEvent(String eventJson) {
        try {
            logger.info("Processing user approved event: {}", eventJson);

            Map<String, Object> event = objectMapper.readValue(eventJson, Map.class);

            if (emailEnabled && Boolean.TRUE.equals(event.get("sendWelcomeEmail"))) {
                String email = (String) event.get("email");
                String firstName = (String) event.get("firstName");
                String userId = (String) event.get("userId");

                emailService.sendWelcomeEmail(email, firstName, userId);
                logger.info("Welcome email sent successfully to: {}", email);
            } else {
                logger.info("Email disabled or not required for event: {}", event.get("eventType"));
            }

        } catch (Exception e) {
            logger.error("Failed to process user approved event: {}", eventJson, e);
            throw new RuntimeException("Failed to process user approved event", e);
        }
    }

    /**
     * Handle user rejected events - send notification email
     */
    @RabbitListener(queues = "user.rejected.queue")
    @CircuitBreaker(name = "emailService", fallbackMethod = "handleRejectedEventFallback")
    public void handleUserRejectedEvent(String eventJson) {
        try {
            logger.info("Processing user rejected event: {}", eventJson);

            Map<String, Object> event = objectMapper.readValue(eventJson, Map.class);

            if (emailEnabled && Boolean.TRUE.equals(event.get("sendNotificationEmail"))) {
                String email = (String) event.get("email");
                String firstName = (String) event.get("firstName");
                String reason = (String) event.get("reason");

                emailService.sendRejectionEmail(email, firstName, reason);
                logger.info("Rejection notification sent successfully to: {}", email);
            } else {
                logger.info("Email disabled or not required for event: {}", event.get("eventType"));
            }

        } catch (Exception e) {
            logger.error("Failed to process user rejected event: {}", eventJson, e);
            throw new RuntimeException("Failed to process user rejected event", e);
        }
    }

    /**
     * Handle user registered events - admin notification
     */
    @RabbitListener(queues = "user.registered.queue")
    public void handleUserRegisteredEvent(String eventJson) {
        try {
            logger.info("Processing user registered event: {}", eventJson);

            Map<String, Object> event = objectMapper.readValue(eventJson, Map.class);

            if (emailEnabled && Boolean.TRUE.equals(event.get("requiresApproval"))) {
                String userEmail = (String) event.get("email");
                String firstName = (String) event.get("firstName");
                String userId = (String) event.get("userId");

                // Send notification to admin about new user registration
                emailService.sendAdminNotification(userEmail, firstName, userId);
                logger.info("Admin notification sent for new user registration: {}", userEmail);
            }

        } catch (Exception e) {
            logger.error("Failed to process user registered event: {}", eventJson, e);
            // Don't throw exception for registration event to avoid blocking user registration
        }
    }

    /**
     * Fallback method for user approved event processing
     */
    public void handleApprovedEventFallback(String eventJson, Exception ex) {
        logger.error("Circuit breaker fallback - failed to process user approved event: {}", eventJson, ex);
        // Could implement alternative notification mechanism here
    }

    /**
     * Fallback method for user rejected event processing
     */
    public void handleRejectedEventFallback(String eventJson, Exception ex) {
        logger.error("Circuit breaker fallback - failed to process user rejected event: {}", eventJson, ex);
        // Could implement alternative notification mechanism here
    }
}
