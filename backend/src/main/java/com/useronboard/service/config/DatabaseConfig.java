package com.useronboard.service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import javax.sql.DataSource;

/**
 * Database configuration that supports both MSSQL and Oracle
 * Validates database type and logs connection information at startup
 */
@Configuration
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${app.database.type:mssql}")
    private String databaseType;

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    private final DataSource dataSource;

    public DatabaseConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateAndLogDatabaseConfiguration() {
        logger.info("=== Database Configuration ===");
        logger.info("Database Type: {}", databaseType);
        logger.info("JDBC URL: {}", maskPassword(jdbcUrl));
        logger.info("Driver Class: {}", driverClassName);

        // Validate database type
        if (!"mssql".equalsIgnoreCase(databaseType) && !"oracle".equalsIgnoreCase(databaseType)) {
            logger.error("Invalid database type: {}. Supported types: mssql, oracle", databaseType);
            throw new IllegalArgumentException("Invalid database type: " + databaseType);
        }

        // Validate driver compatibility
        validateDriverCompatibility();

        logger.info("Database configuration validated successfully");
        logger.info("================================");
    }

    private void validateDriverCompatibility() {
        boolean isValidConfig = false;

        switch (databaseType.toLowerCase()) {
            case "mssql":
                if (driverClassName.contains("sqlserver")) {
                    isValidConfig = true;
                    logger.info("✓ MSSQL configuration detected - using SQL Server driver");
                }
                break;
            case "oracle":
                if (driverClassName.contains("oracle")) {
                    isValidConfig = true;
                    logger.info("✓ Oracle configuration detected - using Oracle driver");
                }
                break;
        }

        if (!isValidConfig) {
            String error = String.format(
                "Database configuration mismatch: DB_TYPE=%s but driver=%s",
                databaseType, driverClassName
            );
            logger.error(error);
            throw new IllegalArgumentException(error);
        }
    }

    private String maskPassword(String jdbcUrl) {
        if (jdbcUrl == null) return "null";

        // Mask password in JDBC URL for logging
        return jdbcUrl.replaceAll("password=[^;;&]*", "password=***");
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public boolean isMssql() {
        return "mssql".equalsIgnoreCase(databaseType);
    }

    public boolean isOracle() {
        return "oracle".equalsIgnoreCase(databaseType);
    }
}
