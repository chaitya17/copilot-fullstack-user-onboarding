-- MSSQL Initial Schema Migration
-- User Onboarding Service - V1 Schema Creation for SQL Server

-- Users table with portable column types
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(20),
    roles VARCHAR(255) DEFAULT 'USER',
    status VARCHAR(16) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACTIVE', 'REJECTED')),
    created_at DATETIME2 DEFAULT GETUTCDATE(),
    updated_at DATETIME2 DEFAULT GETUTCDATE(),
    metadata NVARCHAR(MAX) -- JSON metadata storage for MSSQL
);

-- Refresh tokens table for JWT token revocation support
CREATE TABLE refresh_tokens (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME2 NOT NULL,
    created_at DATETIME2 DEFAULT GETUTCDATE(),
    revoked BIT DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Onboarding steps tracking
CREATE TABLE onboarding_steps (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    step_name VARCHAR(100) NOT NULL,
    step_order INT NOT NULL,
    is_completed BIT DEFAULT 0,
    completed_at DATETIME2,
    step_data NVARCHAR(MAX), -- JSON data for step-specific information
    created_at DATETIME2 DEFAULT GETUTCDATE(),
    updated_at DATETIME2 DEFAULT GETUTCDATE(),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Audit log table for user status changes
CREATE TABLE user_audit_log (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    action VARCHAR(50) NOT NULL, -- CREATED, APPROVED, REJECTED, ACTIVATED, DEACTIVATED
    performed_by VARCHAR(36), -- Admin user ID who performed the action
    old_status VARCHAR(16),
    new_status VARCHAR(16),
    reason NVARCHAR(500),
    created_at DATETIME2 DEFAULT GETUTCDATE(),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for performance optimization
CREATE INDEX IX_users_email ON users(email);
CREATE INDEX IX_users_status ON users(status);
CREATE INDEX IX_users_created_at ON users(created_at);

CREATE INDEX IX_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX IX_refresh_tokens_expires_at ON refresh_tokens(expires_at);
CREATE INDEX IX_refresh_tokens_token_hash ON refresh_tokens(token_hash);

CREATE INDEX IX_onboarding_steps_user_id ON onboarding_steps(user_id);
CREATE INDEX IX_onboarding_steps_user_step_order ON onboarding_steps(user_id, step_order);

CREATE INDEX IX_user_audit_log_user_id ON user_audit_log(user_id);
CREATE INDEX IX_user_audit_log_created_at ON user_audit_log(created_at);

-- Trigger for updating updated_at timestamp (MSSQL specific)
-- Note: This is MSSQL-specific syntax, Oracle version will be different
