CREATE SCHEMA IF NOT EXISTS common_schema;
SET search_path TO common_schema;

-- ==============================================================================
-- GLOBAL SCENARIO RULES
-- ==============================================================================
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
CREATE INDEX IF NOT EXISTS idx_global_scenario_rules_rule_id ON global_scenario_rules(rule_id);

-- ==============================================================================
-- GLOBAL RULE CONDITIONS
-- ==============================================================================
CREATE TABLE IF NOT EXISTS global_rule_conditions (
                                                      id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    rule_id             UUID            NOT NULL,
    attribute_name      VARCHAR(100)    NOT NULL,
    threshold_value     VARCHAR(255)    NOT NULL,
    value_data_type     VARCHAR(50)     NOT NULL,
    sys_created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    sys_updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_global_rule_conditions    PRIMARY KEY (id),
    CONSTRAINT fk_grc_rule FOREIGN KEY (rule_id) REFERENCES global_rules(id) ON DELETE CASCADE
    );

-- ==============================================================================
-- INDEXES & TRIGGERS
-- ==============================================================================

-- Guard for the Trigger (Postgres triggers don't support IF NOT EXISTS directly)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_global_rule_conditions_updated_at') THEN
CREATE TRIGGER trg_global_rule_conditions_updated_at
    BEFORE UPDATE ON global_rule_conditions
    FOR EACH ROW
    EXECUTE FUNCTION update_sys_updated_at_column();
END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_global_rule_conditions_rule_id ON global_rule_conditions(rule_id);