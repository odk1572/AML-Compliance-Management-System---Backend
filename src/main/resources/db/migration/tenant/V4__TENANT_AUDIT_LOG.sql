CREATE TABLE tenant_audit_log (
                                  id                  UUID            NOT NULL DEFAULT gen_random_uuid(),

    -- Actor who performed the action (Bank Admin or CO)
                                  actor_id            UUID            NOT NULL,

    -- High-level classification for filtering and reporting
                                  action_category     VARCHAR(20)     NOT NULL,

    -- Specific action description (human-readable)
                                  action_performed    VARCHAR(150)    NOT NULL,

    -- What was changed
                                  target_entity_type  VARCHAR(60),
                                  target_entity_id    UUID,

    -- State snapshots (JSONB for schema flexibility)
                                  prev_state          JSONB,          -- NULL for CREATE actions
                                  next_state          JSONB,          -- NULL for DELETE / read-only events

    -- Request metadata
                                  ip_address          VARCHAR(45),

    -- Single immutable timestamp — no sys_updated_at on audit logs
                                  sys_created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- ── Constraints ──────────────────────────────────────────
                                  CONSTRAINT pk_tenant_audit_log
                                      PRIMARY KEY (id),

                                  CONSTRAINT chk_tal_category
                                      CHECK (action_category IN (
                                                                 'AUTH',         -- login, logout, password change, account lock
                                                                 'USER_MGMT',    -- create/deactivate CO, reset password
                                                                 'RULE_MGMT',    -- activate scenario, toggle rule, create version
                                                                 'CASE_WORK',    -- create case, add note, assign, escalate, close
                                                                 'BATCH',        -- upload, processing start/complete/fail
                                                                 'REPORT',       -- report viewed or exported
                                                                 'SYSTEM'        -- automated system events
                                          )),

                                  CONSTRAINT chk_tal_action_performed
                                      CHECK (length(action_performed) > 0),

                                  CONSTRAINT fk_tal_actor
                                      FOREIGN KEY (actor_id)
                                          REFERENCES tenant_users (id)
);

CREATE INDEX idx_tal_actor_time
    ON tenant_audit_log (actor_id, sys_created_at DESC);

CREATE INDEX idx_tal_category_time
    ON tenant_audit_log (action_category, sys_created_at DESC);

CREATE INDEX idx_tal_entity
    ON tenant_audit_log (target_entity_type, target_entity_id, sys_created_at DESC)
    WHERE target_entity_id IS NOT NULL;


CREATE INDEX idx_tal_created_at
    ON tenant_audit_log (sys_created_at DESC);

CREATE OR REPLACE RULE no_update_tenant_audit_log AS
    ON UPDATE TO tenant_audit_log
                  DO INSTEAD NOTHING;

CREATE OR REPLACE RULE no_delete_tenant_audit_log AS
    ON DELETE TO tenant_audit_log
    DO INSTEAD NOTHING;
