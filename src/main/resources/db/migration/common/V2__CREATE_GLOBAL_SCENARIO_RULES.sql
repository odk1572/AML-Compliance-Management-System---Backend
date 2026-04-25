-- 1. Setup Schema
CREATE SCHEMA IF NOT EXISTS common_schema;
SET search_path TO common_schema;

-- ==============================================================================
-- TABLE: GLOBAL_SCENARIOS
-- ==============================================================================
CREATE TABLE global_scenarios (
                                  id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
                                  scenario_name       VARCHAR(255)    UNIQUE NOT NULL,
                                  category            VARCHAR(100)    NOT NULL,
                                  description         TEXT,
                                  created_by          UUID,           -- Assuming references platform_users(id) in full DB
                                  sys_is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
                                  sys_deleted_at      TIMESTAMPTZ,
                                  sys_created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                  sys_updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                                  CONSTRAINT pk_global_scenarios PRIMARY KEY (id)
);

CREATE TRIGGER trg_global_scenarios_updated_at
    BEFORE UPDATE ON global_scenarios
    FOR EACH ROW
    EXECUTE FUNCTION update_sys_updated_at_column();

CREATE INDEX idx_global_scenarios_category ON global_scenarios(category);
CREATE INDEX idx_global_scenarios_sys_is_deleted ON global_scenarios(sys_is_deleted);

-- ==============================================================================
-- TABLE: GLOBAL_RULES
-- ==============================================================================
CREATE TABLE global_rules (
                              id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
                              rule_name           VARCHAR(150)    NOT NULL,
                              rule_type           VARCHAR(50)     NOT NULL,   -- Maps to Executor Strategy
                              severity            VARCHAR(10)     NOT NULL DEFAULT 'MEDIUM',
                              base_risk_score     SMALLINT        NOT NULL DEFAULT 50,
                              sys_is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
                              sys_deleted_at      TIMESTAMPTZ,
                              sys_created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                              sys_updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                              CONSTRAINT pk_global_rules          PRIMARY KEY (id),
                              CONSTRAINT chk_global_rules_sev     CHECK (severity IN ('CRITICAL','HIGH','MEDIUM','LOW')),
                              CONSTRAINT chk_global_rules_type    CHECK (rule_type IN (
                                                                                       'STRUCTURING',
                                                                                       'VELOCITY',
                                                                                       'LARGE_TRANSACTION',
                                                                                       'ROUND_AMOUNT',
                                                                                       'PASS_THROUGH',
                                                                                       'FUNNEL',
                                                                                       'SUDDEN_SPIKE',
                                                                                       'DORMANT_REACTIVATION',
                                                                                       'LOW_INCOME_HIGH_TRANSFER'
                                  ))
);

CREATE TRIGGER trg_global_rules_updated_at
    BEFORE UPDATE ON global_rules
    FOR EACH ROW
    EXECUTE FUNCTION update_sys_updated_at_column();

CREATE INDEX idx_global_rules_severity ON global_rules(severity);
CREATE INDEX idx_global_rules_sys_is_deleted ON global_rules(sys_is_deleted);
CREATE INDEX idx_global_rules_type ON global_rules(rule_type); -- Added index for faster strategy lookups