-- MSSQL Admin User Creation
-- Creates default admin user for initial system access
-- TODO: Change the default password hash in production

-- Insert admin user with BCrypt hashed password for 'admin123'
-- Password hash generated with BCrypt strength 12
INSERT INTO users (
    id,
    email,
    password_hash,
    first_name,
    last_name,
    roles,
    status,
    created_at,
    updated_at,
    metadata
) VALUES (
    'admin-001',
    'admin@useronboard.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqyc5rLm2JFn2HKyFwKgKzK', -- 'admin123'
    'System',
    'Administrator',
    'ADMIN,USER',
    'ACTIVE',
    GETUTCDATE(),
    GETUTCDATE(),
    '{"created_by": "system", "initial_setup": true}'
);

-- Create audit log entry for admin user creation
INSERT INTO user_audit_log (
    id,
    user_id,
    action,
    performed_by,
    old_status,
    new_status,
    reason,
    created_at
) VALUES (
    'audit-admin-001',
    'admin-001',
    'CREATED',
    'system',
    NULL,
    'ACTIVE',
    'Initial admin user created during system setup',
    GETUTCDATE()
);
