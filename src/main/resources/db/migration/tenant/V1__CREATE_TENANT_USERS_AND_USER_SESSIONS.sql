CREATE OR REPLACE FUNCTION update_sys_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.sys_updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE tenant_users (
                              id UUID PRIMARY KEY,
                              employee_id VARCHAR(100) UNIQUE NOT NULL,
                              full_name VARCHAR(255) NOT NULL,
                              email VARCHAR(255) UNIQUE NOT NULL,
                              password_hash VARCHAR(255) NOT NULL,
                              role VARCHAR(50) NOT NULL DEFAULT 'COMPLIANCE_OFFICER',
                              is_first_login BOOLEAN NOT NULL DEFAULT TRUE,
                              failed_login_attempts INT NOT NULL DEFAULT 0,
                              is_locked BOOLEAN NOT NULL DEFAULT FALSE,
                              locked_at TIMESTAMP,
                              last_login_at TIMESTAMP,
                              last_login_ip VARCHAR(45),
                              sys_created_by UUID REFERENCES tenant_users(id) ON DELETE SET NULL,

                              sys_is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                              sys_deleted_at TIMESTAMP,
                              sys_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              sys_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_tenant_users_updated_at
    BEFORE UPDATE ON tenant_users
    FOR EACH ROW
    EXECUTE FUNCTION update_sys_updated_at_column();

CREATE INDEX idx_tenant_users_email ON tenant_users(email);
CREATE INDEX idx_tenant_users_employee_id ON tenant_users(employee_id);
CREATE INDEX idx_tenant_users_sys_is_deleted ON tenant_users(sys_is_deleted);
CREATE TABLE user_sessions (
                               id UUID PRIMARY KEY,
                               user_id UUID NOT NULL REFERENCES tenant_users(id) ON DELETE CASCADE,
                               jwt_jti VARCHAR(36) UNIQUE NOT NULL,
                               ip_address VARCHAR(45),
                               user_agent TEXT,
                               expires_at TIMESTAMP NOT NULL,
                               is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
                               revoked_at TIMESTAMP,
                               sys_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_sessions_jti ON user_sessions(jwt_jti);
CREATE INDEX idx_user_sessions_expires_at ON user_sessions(expires_at);