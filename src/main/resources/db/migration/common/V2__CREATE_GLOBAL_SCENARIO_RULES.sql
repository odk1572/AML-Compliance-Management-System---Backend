CREATE SCHEMA IF NOT EXISTS common_schema;
SET search_path TO common_schema;

CREATE TABLE IF NOT EXISTS global_scenarios (
                                                id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    scenario_name       VARCHAR(255)    UNIQUE NOT NULL,
    category            VARCHAR(100)    NOT NULL,
    description         TEXT,
    created_by          UUID,
    sys_is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    sys_deleted_at      TIMESTAMPTZ,
    sys_created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    sys_updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_global_scenarios PRIMARY KEY (id)
    );

CREATE INDEX IF NOT EXISTS idx_global_scenarios_category ON global_scenarios(category);
CREATE INDEX IF NOT EXISTS idx_global_scenarios_sys_is_deleted ON global_scenarios(sys_is_deleted);

CREATE TABLE IF NOT EXISTS global_rules (
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

CREATE INDEX IF NOT EXISTS idx_global_rules_severity ON global_rules(severity);
CREATE INDEX IF NOT EXISTS idx_global_rules_sys_deleted ON global_rules(sys_is_deleted);
CREATE INDEX IF NOT EXISTS idx_global_rules_type ON global_rules(rule_type);

CREATE TABLE IF NOT EXISTS global_rule_conditions (
                                                      id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    rule_id             UUID            NOT NULL,
    attribute_name      VARCHAR(100)    NOT NULL, -- e.g., 'LOOKBACK_WINDOW'
    threshold_value     VARCHAR(255)    NOT NULL, -- e.g., '10000' or '24h'
    value_data_type     VARCHAR(50)     NOT NULL, -- e.g., 'NUMERIC', 'INTERVAL'

    sys_created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    sys_updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_global_rule_conditions PRIMARY KEY (id),
    CONSTRAINT fk_grc_rule FOREIGN KEY (rule_id) REFERENCES global_rules(id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_global_rule_conditions_rule_id ON global_rule_conditions(rule_id);

CREATE TABLE IF NOT EXISTS global_scenario_rules (
                                                     id                  UUID            PRIMARY KEY,
                                                     scenario_id         UUID            NOT NULL REFERENCES global_scenarios(id) ON DELETE CASCADE,
    rule_id             UUID            NOT NULL REFERENCES global_rules(id) ON DELETE CASCADE,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    priority_order      INT             NOT NULL DEFAULT 0,
    sys_created_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_scenario_rule UNIQUE (scenario_id, rule_id)
    );

CREATE INDEX IF NOT EXISTS idx_global_scenario_rules_scenario_id ON global_scenario_rules(scenario_id);

DROP TRIGGER IF EXISTS trg_global_scenarios_updated_at ON global_scenarios;
CREATE TRIGGER trg_global_scenarios_updated_at
    BEFORE UPDATE ON global_scenarios
    FOR EACH ROW EXECUTE FUNCTION update_sys_updated_at_column();

DROP TRIGGER IF EXISTS trg_global_rules_updated_at ON global_rules;
CREATE TRIGGER trg_global_rules_updated_at
    BEFORE UPDATE ON global_rules
    FOR EACH ROW EXECUTE FUNCTION update_sys_updated_at_column();

DROP TRIGGER IF EXISTS trg_global_rule_conditions_updated_at ON global_rule_conditions;
CREATE TRIGGER trg_global_rule_conditions_updated_at
    BEFORE UPDATE ON global_rule_conditions
    FOR EACH ROW EXECUTE FUNCTION update_sys_updated_at_column();