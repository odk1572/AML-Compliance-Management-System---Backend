
-- ── 1. TENANT_USERS ──────────────────────────────────────────
CREATE TABLE tenant_users (
    id                      UUID            NOT NULL DEFAULT gen_random_uuid(),

    -- Identity
    employee_id             VARCHAR(50)     NOT NULL,
    full_name               VARCHAR(150)    NOT NULL,
    email                   VARCHAR(255)    NOT NULL,
    password_hash           VARCHAR(255)    NOT NULL,
    role                    VARCHAR(25)     NOT NULL,

    -- First-login gate: AuthService blocks all non-change-password
    -- calls while this flag is TRUE. Set FALSE after first reset.
    is_first_login          BOOLEAN         NOT NULL DEFAULT TRUE,

    -- Brute-force protection
    failed_login_attempts   SMALLINT        NOT NULL DEFAULT 0,
    is_locked               BOOLEAN         NOT NULL DEFAULT FALSE,
    locked_at               TIMESTAMPTZ,

    -- Session metadata (populated on successful login)
    last_login_at           TIMESTAMPTZ,
    last_login_ip           VARCHAR(45),        -- supports IPv6

    -- Lineage: who created this user (Bank Admin ID)
    -- Self-referential — DEFERRABLE to allow Bank Admin seed insert
    sys_created_by          UUID,

    -- Soft delete
    sys_is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    sys_deleted_at          TIMESTAMPTZ,

    -- Audit timestamps
    sys_created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    sys_updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- ── Constraints ──────────────────────────────────────────
    CONSTRAINT pk_tenant_users
        PRIMARY KEY (id),

    CONSTRAINT uq_tenant_users_email
        UNIQUE (email),

    CONSTRAINT uq_tenant_users_employee_id
        UNIQUE (employee_id),

    CONSTRAINT chk_tenant_users_role
        CHECK (role IN ('BANK_ADMIN', 'COMPLIANCE_OFFICER')),

    CONSTRAINT chk_tenant_users_failed_attempts
        CHECK (failed_login_attempts >= 0),

    -- Self-referential FK — deferred so Bank Admin can be inserted
    -- in the same transaction that seeds the tenant schema
    CONSTRAINT fk_tenant_users_created_by
        FOREIGN KEY (sys_created_by)
        REFERENCES tenant_users (id)
        DEFERRABLE INITIALLY DEFERRED
);

-- ── Indexes ───────────────────────────────────────────────────
-- Login lookup — hit on every authentication request
CREATE UNIQUE INDEX idx_tenant_users_email_active
    ON tenant_users (email)
    WHERE sys_is_deleted = FALSE;

-- Role-based queries (Bank Admin lists all COs, workload reports)
CREATE INDEX idx_tenant_users_role
    ON tenant_users (role)
    WHERE sys_is_deleted = FALSE;

-- Locked account monitoring
CREATE INDEX idx_tenant_users_locked
    ON tenant_users (is_locked)
    WHERE is_locked = TRUE AND sys_is_deleted = FALSE;



-- ── 2. USER_SESSIONS ──────────────────────────────────────────
CREATE TABLE user_sessions (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL,

    -- UUID string from JWT jti claim — globally unique per token
    jwt_jti         VARCHAR(36)     NOT NULL,

    -- Request metadata captured at login
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),

    -- Token lifecycle
    expires_at      TIMESTAMPTZ     NOT NULL,
    is_revoked      BOOLEAN         NOT NULL DEFAULT FALSE,
    revoked_at      TIMESTAMPTZ,    -- populated by JtiBlacklistService.revoke()

    -- Immutable creation timestamp — no sys_updated_at
    -- (only is_revoked + revoked_at are ever written after insert)
    sys_created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- ── Constraints ──────────────────────────────────────────
    CONSTRAINT pk_user_sessions
        PRIMARY KEY (id),

    CONSTRAINT uq_user_sessions_jti
        UNIQUE (jwt_jti),

    CONSTRAINT chk_user_sessions_revoked_at
        CHECK (
            (is_revoked = FALSE AND revoked_at IS NULL) OR
            (is_revoked = TRUE  AND revoked_at IS NOT NULL)
        ),

    CONSTRAINT fk_user_sessions_user
        FOREIGN KEY (user_id)
        REFERENCES tenant_users (id)
);

-- ── Indexes ───────────────────────────────────────────────────
-- JtiBlacklistService hits this index on EVERY authenticated request.
-- Must be fast — it is on the hot path of the security filter chain.
CREATE UNIQUE INDEX idx_user_sessions_jti
    ON user_sessions (jwt_jti);

-- Partial index for active (non-revoked) sessions.
-- TenantSchemaDeactivator uses this to bulk-revoke on bank suspension.
CREATE INDEX idx_user_sessions_user_active
    ON user_sessions (user_id)
    WHERE is_revoked = FALSE;

-- Cleanup job: find expired sessions for archival
CREATE INDEX idx_user_sessions_expires
    ON user_sessions (expires_at)
    WHERE is_revoked = FALSE;

CREATE TRIGGER trg_update_tenant_users_timestamp
    BEFORE UPDATE ON tenant_users
    FOR EACH ROW EXECUTE FUNCTION update_timestamp_column();