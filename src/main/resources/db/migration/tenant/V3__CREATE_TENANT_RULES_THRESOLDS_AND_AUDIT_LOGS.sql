-- ============================================================
-- TABLE: tenant_rule_thresholds
-- Aligned with CHANGE 2 OF 4: Semantic parameter overrides
-- ============================================================
CREATE TABLE tenant_rule_thresholds (
                                        id                          UUID            PRIMARY KEY,

    -- Links to the tenant-specific rule instance
                                        tenant_rule_id              UUID            NOT NULL REFERENCES tenant_rules(id) ON DELETE CASCADE,

    -- Cross-Schema FK: Points to the specific semantic condition (NONE, SUM, COUNT, etc.)
    -- We use RESTRICT to prevent deleting a global condition that is currently tuned by a bank.
                                        global_condition_id         UUID            NOT NULL REFERENCES common_schema.global_rule_conditions(id) ON DELETE RESTRICT,

    -- The tuned threshold value (e.g., overriding 10,000 with 15,000)
                                        override_value              VARCHAR(255),

    -- The tuned time window (e.g., overriding 24h with 48h)
    -- Matches the VARCHAR(10) length in common_schema
                                        override_lookback_period    VARCHAR(10),

    -- Note: override_aggregation_function is REMOVED.
    -- Tenants tune the limits, they do not change the core execution logic.

                                        sys_created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        sys_updated_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Ensures a tenant cannot have two different overrides for the same semantic parameter
                                        CONSTRAINT uk_tenant_rule_condition UNIQUE (tenant_rule_id, global_condition_id)
);

-- Index for performance when fetching all overrides for a specific rule during execution
CREATE INDEX idx_tenant_rule_thresholds_rule_id ON tenant_rule_thresholds(tenant_rule_id);

-- Trigger to maintain the sys_updated_at audit column
CREATE TRIGGER trg_tenant_rule_thresholds_updated_at
    BEFORE UPDATE ON tenant_rule_thresholds
    FOR EACH ROW
    EXECUTE FUNCTION update_sys_updated_at_column();


-- ============================================================
-- TABLE: tenant_audit_log
-- Standardized for Case Management and Rule Tuning audits
-- ============================================================
CREATE TABLE tenant_audit_log (
                                  id                  UUID            PRIMARY KEY,
                                  actor_id            UUID            REFERENCES tenant_users(id) ON DELETE SET NULL,

    -- e.g., 'RULE_TUNING', 'BATCH_UPLOAD', 'ALERT_DISPOSITION'
                                  action_category     VARCHAR(50)     NOT NULL,
                                  action_performed    VARCHAR(255)    NOT NULL,

    -- e.g., 'TENANT_RULE_THRESHOLD', 'TRANSACTION_BATCH'
                                  target_entity_type  VARCHAR(100)    NOT NULL,
                                  target_entity_id    UUID,

    -- Stores the previous/new values for the auditor (e.g., old threshold vs new)
                                  prev_state          JSONB,
                                  next_state          JSONB,

                                  ip_address          VARCHAR(45),
                                  sys_created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tenant_audit_actor_id ON tenant_audit_log(actor_id);
CREATE INDEX idx_tenant_audit_category ON tenant_audit_log(action_category);
CREATE INDEX idx_tenant_audit_target_entity ON tenant_audit_log(target_entity_type, target_entity_id);
CREATE INDEX idx_tenant_audit_created_at ON tenant_audit_log(sys_created_at);