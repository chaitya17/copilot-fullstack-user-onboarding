-- Flyway Migration V1: Initial Schema
-- This script works for both MSSQL and Oracle with minor syntax differences
-- Database-specific versions should be created based on DB_TYPE

-- MSSQL Version (default)
-- For MSSQL, use this syntax:

CREATE TABLE users (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    username NVARCHAR(50) UNIQUE NOT NULL,
    email NVARCHAR(100) UNIQUE NOT NULL,
    password_hash NVARCHAR(255) NOT NULL,
    first_name NVARCHAR(50) NOT NULL,
    last_name NVARCHAR(50) NOT NULL,
    phone NVARCHAR(20),
    date_of_birth DATE,
    created_at DATETIME2 DEFAULT GETUTCDATE(),
    updated_at DATETIME2 DEFAULT GETUTCDATE(),
    is_active BIT DEFAULT 1,
    email_verified BIT DEFAULT 0
);

CREATE TABLE roles (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(50) UNIQUE NOT NULL,
    description NVARCHAR(255),
    created_at DATETIME2 DEFAULT GETUTCDATE()
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at DATETIME2 DEFAULT GETUTCDATE(),
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE TABLE onboarding_steps (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    step_name NVARCHAR(100) NOT NULL,
    step_order INT NOT NULL,
    is_completed BIT DEFAULT 0,
    completed_at DATETIME2,
    data NVARCHAR(MAX), -- JSON data for step-specific information
    created_at DATETIME2 DEFAULT GETUTCDATE(),
    updated_at DATETIME2 DEFAULT GETUTCDATE(),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Insert default roles
INSERT INTO roles (name, description) VALUES
('USER', 'Standard user role'),
('ADMIN', 'Administrator role'),
('MODERATOR', 'Moderator role');

-- Create indexes for performance
CREATE INDEX IX_users_email ON users(email);
CREATE INDEX IX_users_username ON users(username);
CREATE INDEX IX_onboarding_steps_user_id ON onboarding_steps(user_id);
CREATE INDEX IX_onboarding_steps_step_order ON onboarding_steps(user_id, step_order);

/*
ORACLE VERSION - Use this when DB_TYPE=oracle
Uncomment and modify as needed:

CREATE TABLE users (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username VARCHAR2(50) UNIQUE NOT NULL,
    email VARCHAR2(100) UNIQUE NOT NULL,
    password_hash VARCHAR2(255) NOT NULL,
    first_name VARCHAR2(50) NOT NULL,
    last_name VARCHAR2(50) NOT NULL,
    phone VARCHAR2(20),
    date_of_birth DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active NUMBER(1) DEFAULT 1 CHECK (is_active IN (0,1)),
    email_verified NUMBER(1) DEFAULT 0 CHECK (email_verified IN (0,1))
);

CREATE TABLE roles (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR2(50) UNIQUE NOT NULL,
    description VARCHAR2(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
    user_id NUMBER NOT NULL,
    role_id NUMBER NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE TABLE onboarding_steps (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id NUMBER NOT NULL,
    step_name VARCHAR2(100) NOT NULL,
    step_order NUMBER NOT NULL,
    is_completed NUMBER(1) DEFAULT 0 CHECK (is_completed IN (0,1)),
    completed_at TIMESTAMP,
    data CLOB, -- JSON data for step-specific information
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Insert default roles (Oracle version)
INSERT INTO roles (name, description) VALUES ('USER', 'Standard user role');
INSERT INTO roles (name, description) VALUES ('ADMIN', 'Administrator role');
INSERT INTO roles (name, description) VALUES ('MODERATOR', 'Moderator role');

-- Create indexes (Oracle version)
CREATE INDEX IX_users_email ON users(email);
CREATE INDEX IX_users_username ON users(username);
CREATE INDEX IX_onboarding_steps_user_id ON onboarding_steps(user_id);
CREATE INDEX IX_onboarding_steps_step_order ON onboarding_steps(user_id, step_order);

*/
