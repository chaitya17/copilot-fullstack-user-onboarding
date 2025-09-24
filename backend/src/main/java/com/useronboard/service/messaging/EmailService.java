package com.useronboard.service.messaging;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Email service with resilience patterns
 * Handles welcome emails, rejection notifications, and admin alerts
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.email.from:noreply@useronboard.com}")
    private String fromEmail;

    @Value("${app.email.admin:admin@useronboard.com}")
    private String adminEmail;

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Send welcome email to approved user
     */
    @CircuitBreaker(name = "emailService")
    @Retry(name = "emailService")
    public void sendWelcomeEmail(String toEmail, String firstName, String userId) {
        if (!emailEnabled) {
            logger.debug("Email disabled - skipping welcome email to: {}", toEmail);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Welcome to User Onboard - Account Approved!");

            String emailBody = String.format(
                "Dear %s,\n\n" +
                "Congratulations! Your account has been approved and you can now access the User Onboard platform.\n\n" +
                "You can now log in using your registered email address.\n\n" +
                "If you have any questions, please contact our support team.\n\n" +
                "Best regards,\n" +
                "The User Onboard Team\n\n" +
                "User ID: %s",
                firstName != null ? firstName : "User",
                userId
            );

            message.setText(emailBody);

            mailSender.send(message);
            logger.info("Welcome email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            logger.error("Failed to send welcome email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send welcome email", e);
        }
    }

    /**
     * Send rejection notification email
     */
    @CircuitBreaker(name = "emailService")
    @Retry(name = "emailService")
    public void sendRejectionEmail(String toEmail, String firstName, String reason) {
        if (!emailEnabled) {
            logger.debug("Email disabled - skipping rejection email to: {}", toEmail);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("User Onboard - Account Application Update");

            String emailBody = String.format(
                "Dear %s,\n\n" +
                "Thank you for your interest in the User Onboard platform.\n\n" +
                "After careful review, we regret to inform you that your account application has not been approved at this time.\n\n" +
                "%s\n\n" +
                "You may reapply in the future or contact our support team if you have any questions.\n\n" +
                "Best regards,\n" +
                "The User Onboard Team",
                firstName != null ? firstName : "User",
                reason != null && !reason.isEmpty() ? "Reason: " + reason : "Please contact support for more details."
            );

            message.setText(emailBody);

            mailSender.send(message);
            logger.info("Rejection email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            logger.error("Failed to send rejection email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send rejection email", e);
        }
    }

    /**
     * Send admin notification about new user registration
     */
    @CircuitBreaker(name = "emailService")
    public void sendAdminNotification(String userEmail, String firstName, String userId) {
        if (!emailEnabled) {
            logger.debug("Email disabled - skipping admin notification");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(adminEmail);
            message.setSubject("New User Registration - Approval Required");

            String emailBody = String.format(
                "A new user has registered and requires approval:\n\n" +
                "Name: %s\n" +
                "Email: %s\n" +
                "User ID: %s\n\n" +
                "Please review and approve/reject this user in the admin panel.\n\n" +
                "Admin Panel: [Your Admin Panel URL]\n\n" +
                "Best regards,\n" +
                "User Onboard System",
                firstName != null ? firstName : "Not provided",
                userEmail,
                userId
            );

            message.setText(emailBody);

            mailSender.send(message);
            logger.info("Admin notification sent for user registration: {}", userEmail);

        } catch (Exception e) {
            logger.error("Failed to send admin notification for user: {}", userEmail, e);
            // Don't throw exception for admin notifications to avoid blocking user registration
        }
    }
}
