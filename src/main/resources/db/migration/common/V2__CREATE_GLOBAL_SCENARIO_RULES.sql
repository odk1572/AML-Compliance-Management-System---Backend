
CREATE SCHEMA IF NOT EXISTS common_schema;
SET search_path TO common_schema;

CREATE TABLE global_scenarios (
                                  id UUID PRIMARY KEY,
                                  scenario_name VARCHAR(255) UNIQUE NOT NULL,
                                  category VARCHAR(100) NOT NULL,
                                  description TEXT,
                                  created_by UUID REFERENCES platform_users(id),
                                  sys_is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                                  sys_deleted_at TIMESTAMP,
                                  sys_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  sys_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_global_scenarios_updated_at
    BEFORE UPDATE ON global_scenarios
    FOR EACH ROW
    EXECUTE FUNCTION update_sys_updated_at_column();

CREATE INDEX idx_global_scenarios_category ON global_scenarios(category);
CREATE INDEX idx_global_scenarios_sys_is_deleted ON global_scenarios(sys_is_deleted);


CREATE TABLE global_rules (
                              id UUID PRIMARY KEY,
                              rule_name VARCHAR(255) NOT NULL,
                              condition_logic VARCHAR(255) NOT NULL DEFAULT 'AND',
                              severity VARCHAR(50) NOT NULL,
                              base_risk_score INT NOT NULL DEFAULT 0,
                              sys_is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                              sys_deleted_at TIMESTAMP,
                              sys_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              sys_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_global_rules_updated_at
    BEFORE UPDATE ON global_rules
    FOR EACH ROW
    EXECUTE FUNCTION update_sys_updated_at_column();

CREATE INDEX idx_global_rules_severity ON global_rules(severity);
CREATE INDEX idx_global_rules_sys_is_deleted ON global_rules(sys_is_deleted);