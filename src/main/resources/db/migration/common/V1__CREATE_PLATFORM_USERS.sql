
CREATE SCHEMA IF NOT EXISTS common_schema;

SET search_path TO common_schema;

CREATE TABLE platform_users (
    id                      UUID            NOT NULL DEFAULT gen_random_uuid(),
    email                   VARCHAR(255)    NOT NULL,
    password_hash           VARCHAR(255)    NOT NULL,
    full_name               VARCHAR(150)    NOT NULL,
    role                    VARCHAR(30)     NOT NULL DEFAULT 'SUPER_ADMIN',

    -- Login tracking
    failed_login_attempts   SMALLINT        NOT NULL DEFAULT 0,
    is_locked               BOOLEAN         NOT NULL DEFAULT FALSE,
    locked_at               TIMESTAMPTZ,
    last_login_at           TIMESTAMPTZ,
    last_login_ip           VARCHAR(45),                        -- supports IPv6

    -- Soft delete
    sys_is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    sys_deleted_at          TIMESTAMPTZ,

    -- Audit
    sys_created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    sys_updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_platform_users        PRIMARY KEY (id),
    CONSTRAINT uq_platform_users_email  UNIQUE (email),
    CONSTRAINT chk_platform_users_role  CHECK (role IN ('SUPER_ADMIN'))
);

-- Speeds up login lookup
CREATE INDEX idx_platform_users_email
    ON platform_users (email)
    WHERE sys_is_deleted = FALSE;

COMMENT ON TABLE  platform_users                    IS 'Super Admin accounts — common_schema only. Created at deployment via V9 seed.';
COMMENT ON COLUMN platform_users.failed_login_attempts IS 'Reset to 0 on successful login. Account locked at 5.';
COMMENT ON COLUMN platform_users.last_login_ip      IS 'IPv4 or IPv6. Stored for audit trail.';


CREATE OR REPLACE FUNCTION update_timestamp_column()
RETURNS TRIGGER AS $$
BEGIN
   NEW.sys_updated_at = NOW();
RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trg_update_platform_users_timestamp
    BEFORE UPDATE ON platform_users
    FOR EACH ROW EXECUTE FUNCTION update_timestamp_column();