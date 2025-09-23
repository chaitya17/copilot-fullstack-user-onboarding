package com.useronboard.service.entity;

/**
 * User status enumeration for the onboarding workflow
 * Values are stored as VARCHAR(16) for database portability
 */
public enum UserStatus {
    /**
     * User has registered but not yet approved by admin
     */
    PENDING,

    /**
     * User has been approved and can access the system
     */
    ACTIVE,

    /**
     * User registration has been rejected by admin
     */
    REJECTED
}
