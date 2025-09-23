-- Oracle Initial Schema Migration
-- User Onboarding Service - V1 Schema Creation for Oracle

-- Users table with Oracle-specific column types
CREATE TABLE users (
    id VARCHAR2(36) PRIMARY KEY,
    email VARCHAR2(255) UNIQUE NOT NULL,
    password_hash VARCHAR2(255) NOT NULL,
    first_name VARCHAR2(100),
    last_name VARCHAR2(100),
    phone VARCHAR2(20),
    roles VARCHAR2(255) DEFAULT 'USER',
    status VARCHAR2(16) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACTIVE', 'REJECTED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata CLOB -- JSON metadata storage for Oracle (using CLOB)
);

-- Refresh tokens table for JWT token revocation support
CREATE TABLE refresh_tokens (
    id VARCHAR2(36) PRIMARY KEY,
    user_id VARCHAR2(36) NOT NULL,
    token_hash VARCHAR2(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    revoked NUMBER(1) DEFAULT 0 CHECK (revoked IN (0,1)), -- Oracle boolean as NUMBER(1)
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Onboarding steps tracking
CREATE TABLE onboarding_steps (
    id VARCHAR2(36) PRIMARY KEY,
    user_id VARCHAR2(36) NOT NULL,
    step_name VARCHAR2(100) NOT NULL,
    step_order NUMBER NOT NULL,
    is_completed NUMBER(1) DEFAULT 0 CHECK (is_completed IN (0,1)), -- Oracle boolean
    completed_at TIMESTAMP,
    step_data CLOB, -- JSON data for step-specific information
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Audit log table for user status changes
CREATE TABLE user_audit_log (
    id VARCHAR2(36) PRIMARY KEY,
    user_id VARCHAR2(36) NOT NULL,
    action VARCHAR2(50) NOT NULL, -- CREATED, APPROVED, REJECTED, ACTIVATED, DEACTIVATED
    performed_by VARCHAR2(36), -- Admin user ID who performed the action
    old_status VARCHAR2(16),
    new_status VARCHAR2(16),
    reason VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for performance optimization (Oracle syntax)
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

-- Oracle-specific trigger for updating updated_at timestamp
CREATE OR REPLACE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
BEGIN
    :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

CREATE OR REPLACE TRIGGER trg_onboarding_steps_updated_at
    BEFORE UPDATE ON onboarding_steps
    FOR EACH ROW
BEGIN
    :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/
