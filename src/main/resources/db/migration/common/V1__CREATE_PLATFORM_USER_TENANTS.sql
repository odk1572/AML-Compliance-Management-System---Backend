
CREATE SCHEMA IF NOT EXISTS common_schema;
SET search_path TO common_schema;


CREATE OR REPLACE FUNCTION update_sys_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.sys_updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TABLE platform_users (
                                id UUID PRIMARY KEY,
                                email VARCHAR(255) UNIQUE NOT NULL,
                                password_hash VARCHAR(255) NOT NULL,
                                full_name VARCHAR(255) NOT NULL,
                                is_first_login BOOLEAN NOT NULL DEFAULT TRUE,
                                role VARCHAR(50) NOT NULL DEFAULT 'SUPER_ADMIN',
                                failed_login_attempts INT NOT NULL DEFAULT 0,
                                is_locked BOOLEAN NOT NULL DEFAULT FALSE,
                                locked_at TIMESTAMP,
                                last_login_at TIMESTAMP,
                                last_login_ip VARCHAR(45),
                                sys_is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                                sys_deleted_at TIMESTAMP,
                                sys_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                sys_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_platform_users_updated_at
    BEFORE UPDATE ON platform_users
    FOR EACH ROW
    EXECUTE FUNCTION update_sys_updated_at_column();

CREATE INDEX idx_platform_users_email ON platform_users(email);

CREATE TABLE tenants (
                         id UUID PRIMARY KEY,
                         tenant_code VARCHAR(50) UNIQUE NOT NULL,
                         schema_name VARCHAR(63) UNIQUE NOT NULL,
                         institution_name VARCHAR(255) NOT NULL,
                         country_code VARCHAR(3) NOT NULL,
                         regulatory_jurisdiction VARCHAR(100),
                         contact_email VARCHAR(255),
                         contact_phone VARCHAR(50),
                         address TEXT,
                         status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
                         sys_is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                         sys_deleted_at TIMESTAMP,
                         sys_deleted_by UUID REFERENCES platform_users(id),
                         sys_created_by UUID REFERENCES platform_users(id),
                         sys_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         sys_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_tenants_updated_at
    BEFORE UPDATE ON tenants
    FOR EACH ROW
    EXECUTE FUNCTION update_sys_updated_at_column();

CREATE INDEX idx_tenants_tenant_code ON tenants(tenant_code);
CREATE INDEX idx_tenants_schema_name ON tenants(schema_name);
CREATE INDEX idx_tenants_status ON tenants(status);
CREATE INDEX idx_tenants_sys_is_deleted ON tenants(sys_is_deleted);

CREATE TABLE platform_user_sessions (
                                        id UUID PRIMARY KEY,
                                        user_id UUID NOT NULL REFERENCES platform_users(id) ON DELETE CASCADE,
                                        jwt_jti VARCHAR(36) UNIQUE NOT NULL,
                                        ip_address VARCHAR(45),
                                        user_agent TEXT,
                                        expires_at TIMESTAMP NOT NULL,
                                        is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
                                        revoked_at TIMESTAMP,
                                        sys_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        sys_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

);


CREATE INDEX idx_platform_user_sessions_jti ON platform_user_sessions(jwt_jti);
CREATE INDEX idx_platform_user_sessions_expires_at ON platform_user_sessions(expires_at);

