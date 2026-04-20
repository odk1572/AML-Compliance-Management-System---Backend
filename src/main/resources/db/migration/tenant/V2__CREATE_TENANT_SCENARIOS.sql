
CREATE TABLE tenant_scenarios (
                                  id UUID PRIMARY KEY,

                                  global_scenario_id UUID NOT NULL REFERENCES common_schema.global_scenarios(id) ON DELETE RESTRICT,

                                  status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
                                  sys_activated_by UUID REFERENCES tenant_users(id) ON DELETE SET NULL,
                                  sys_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  sys_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  CONSTRAINT uk_tenant_global_scenario UNIQUE (global_scenario_id)
);

CREATE TRIGGER trg_tenant_scenarios_updated_at
    BEFORE UPDATE ON tenant_scenarios
    FOR EACH ROW
    EXECUTE FUNCTION update_sys_updated_at_column();

CREATE INDEX idx_tenant_scenarios_status ON tenant_scenarios(status);

CREATE TABLE tenant_rules (
                              id UUID PRIMARY KEY,
                              tenant_scenario_id UUID NOT NULL REFERENCES tenant_scenarios(id) ON DELETE CASCADE,

                              global_rule_id UUID NOT NULL REFERENCES common_schema.global_rules(id) ON DELETE RESTRICT,

                              rule_code VARCHAR(100) UNIQUE NOT NULL, -- e.g. BA-STR-001 (Bank custom code)
                              rule_name VARCHAR(255) NOT NULL,        -- Bank custom name
                              is_active BOOLEAN NOT NULL DEFAULT TRUE,

                              sys_created_by UUID REFERENCES tenant_users(id) ON DELETE SET NULL,
                              sys_is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                              sys_deleted_at TIMESTAMP,
                              sys_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              sys_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_tenant_rules_updated_at
    BEFORE UPDATE ON tenant_rules
    FOR EACH ROW
    EXECUTE FUNCTION update_sys_updated_at_column();

CREATE INDEX idx_tenant_rules_scenario_id ON tenant_rules(tenant_scenario_id);
CREATE INDEX idx_tenant_rules_is_active ON tenant_rules(is_active);
CREATE INDEX idx_tenant_rules_sys_is_deleted ON tenant_rules(sys_is_deleted);