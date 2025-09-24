package com.useronboard.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * User Onboard Service - Main Application Class
 * Supports dual database configuration (MSSQL/Oracle)
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@EnableTransactionManagement
public class UserOnboardServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserOnboardServiceApplication.class, args);
    }
}
