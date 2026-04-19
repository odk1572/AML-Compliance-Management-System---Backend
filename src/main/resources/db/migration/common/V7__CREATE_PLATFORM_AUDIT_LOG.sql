
SET search_path TO common_schema;

CREATE TABLE platform_audit_log (
                                    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
                                    actor_id            UUID,           -- NULL if action triggered by system/scheduler
                                    actor_role          VARCHAR(30)     NOT NULL DEFAULT 'SUPER_ADMIN',

    -- Tenant context (optional — set when action is tenant-scoped)
                                    tenant_id           UUID,
                                    schema_name         VARCHAR(63),

    -- Action classification
                                    action_category     VARCHAR(30)     NOT NULL,
                                    action_performed    VARCHAR(150)    NOT NULL,

    -- Target entity
                                    target_entity_type  VARCHAR(60),
                                    target_entity_id    UUID,

    -- State snapshot (JSONB for flexible schema evolution)
                                    previous_state      JSONB,
                                    new_state           JSONB,

    -- Request metadata
                                    ip_address          VARCHAR(45),
                                    user_agent          VARCHAR(500),

    -- Single immutable timestamp — no sys_updated_at on audit logs
                                    sys_created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                                    CONSTRAINT pk_platform_audit_log        PRIMARY KEY (id),
                                    CONSTRAINT chk_pal_category CHECK (action_category IN (
                                                                                           'AUTH','RULE','TENANT','WATCHLIST','CONFIG','REPORT','GEO_RISK','SYSTEM')),
                                    CONSTRAINT fk_pal_actor     FOREIGN KEY (actor_id)  REFERENCES platform_users(id),
                                    CONSTRAINT fk_pal_tenant    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

-- Range-partitioned by sys_created_at would be ideal for large deployments.
-- For MVP: partial indexes covering most common query patterns.
CREATE INDEX idx_pal_actor       ON platform_audit_log (actor_id, sys_created_at DESC);
CREATE INDEX idx_pal_tenant      ON platform_audit_log (tenant_id, sys_created_at DESC)
    WHERE tenant_id IS NOT NULL;
CREATE INDEX idx_pal_category    ON platform_audit_log (action_category, sys_created_at DESC);
CREATE INDEX idx_pal_entity      ON platform_audit_log (target_entity_type, target_entity_id)
    WHERE target_entity_id IS NOT NULL;

-- ── IMMUTABILITY ENFORCEMENT ──────────────────────────────────
-- PostgreSQL RULE blocks UPDATE and DELETE at the storage layer.
-- This is enforced BELOW the application layer — even a SUPER_ADMIN
-- database user cannot modify audit log rows.

CREATE OR REPLACE RULE no_update_platform_audit_log AS
    ON UPDATE TO platform_audit_log DO INSTEAD NOTHING;

CREATE OR REPLACE RULE no_delete_platform_audit_log AS
    ON DELETE TO platform_audit_log DO INSTEAD NOTHING;

COMMENT ON TABLE  platform_audit_log IS 'Append-only ledger for Super Admin actions. UPDATE and DELETE blocked via PostgreSQL RULEs. 5-year retention required.';
COMMENT ON COLUMN platform_audit_log.previous_state IS 'JSONB snapshot of entity state before the action. NULL for CREATE actions.';
COMMENT ON COLUMN platform_audit_log.new_state      IS 'JSONB snapshot of entity state after the action. NULL for DELETE actions.';