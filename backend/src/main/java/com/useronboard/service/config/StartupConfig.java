package com.useronboard.service.config;

import com.useronboard.service.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Application startup configuration
 * Initializes JWT keys and validates configuration
 */
@Component
public class StartupConfig {

    private static final Logger logger = LoggerFactory.getLogger(StartupConfig.class);

    private final JwtUtil jwtUtil;
    private final DatabaseConfig databaseConfig;

    public StartupConfig(JwtUtil jwtUtil, DatabaseConfig databaseConfig) {
        this.jwtUtil = jwtUtil;
        this.databaseConfig = databaseConfig;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("=== User Onboard Service Starting ===");

        try {
            // Initialize JWT keys
            jwtUtil.initializeKeys();
            logger.info("✓ JWT keys initialized successfully");

            // Database configuration is validated in DatabaseConfig
            logger.info("✓ Database configuration validated");

            logger.info("✓ Application started successfully");
            logger.info("=====================================");

        } catch (Exception e) {
            logger.error("❌ Application startup failed", e);
            throw new RuntimeException("Application startup failed", e);
        }
    }
}
