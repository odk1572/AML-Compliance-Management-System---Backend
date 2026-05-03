CREATE TABLE tenant_rule_thresholds (
                                        id                          UUID            PRIMARY KEY,

                                        tenant_rule_id              UUID            NOT NULL REFERENCES tenant_rules(id) ON DELETE CASCADE,

                                        global_condition_id         UUID            NOT NULL REFERENCES common_schema.global_rule_conditions(id) ON DELETE RESTRICT,

                                        override_value              VARCHAR(255),
                                        override_lookback_period    VARCHAR(10),

                                        sys_created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        sys_updated_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                        CONSTRAINT uk_tenant_rule_condition UNIQUE (tenant_rule_id, global_condition_id)
);

CREATE INDEX idx_tenant_rule_thresholds_rule_id ON tenant_rule_thresholds(tenant_rule_id);

CREATE TRIGGER trg_tenant_rule_thresholds_updated_at
    BEFORE UPDATE ON tenant_rule_thresholds
    FOR EACH ROW
    EXECUTE FUNCTION update_sys_updated_at_column();


CREATE TABLE tenant_audit_log (
                                  id                  UUID            PRIMARY KEY,
                                  actor_id            UUID            REFERENCES tenant_users(id) ON DELETE SET NULL,

                                  action_category     VARCHAR(50)     NOT NULL,
                                  action_performed    VARCHAR(255)    NOT NULL,


                                  target_entity_type  VARCHAR(100)    NOT NULL,
                                  target_entity_id    UUID,

                                  prev_state          JSONB,
                                  next_state          JSONB,

                                  ip_address          VARCHAR(45),
                                  sys_created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tenant_audit_actor_id ON tenant_audit_log(actor_id);
CREATE INDEX idx_tenant_audit_category ON tenant_audit_log(action_category);
CREATE INDEX idx_tenant_audit_target_entity ON tenant_audit_log(target_entity_type, target_entity_id);
CREATE INDEX idx_tenant_audit_created_at ON tenant_audit_log(sys_created_at);