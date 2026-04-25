

CREATE SCHEMA IF NOT EXISTS common_schema;
SET search_path TO common_schema;


CREATE TABLE global_scenario_rules (
                                       id UUID PRIMARY KEY,
                                       scenario_id UUID NOT NULL REFERENCES global_scenarios(id) ON DELETE CASCADE,
                                       rule_id UUID NOT NULL REFERENCES global_rules(id) ON DELETE CASCADE,
                                       is_active BOOLEAN NOT NULL DEFAULT TRUE,
                                       priority_order INT NOT NULL DEFAULT 0,
                                       sys_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,


                                       CONSTRAINT uk_scenario_rule UNIQUE (scenario_id, rule_id)
);

CREATE INDEX idx_global_scenario_rules_scenario_id ON global_scenario_rules(scenario_id);
CREATE INDEX idx_global_scenario_rules_rule_id ON global_scenario_rules(rule_id);




CREATE TABLE global_rule_conditions (
                                        id                      UUID            NOT NULL DEFAULT gen_random_uuid(),
                                        rule_id                 UUID            NOT NULL,
    -- SEMANTIC PARAMETER TAG — executors use this to identify which parameter
    -- this condition represents within the rule's execution logic
                                        aggregation_function    VARCHAR(10)     NOT NULL DEFAULT 'NONE',
    -- lookback_period used by executors that need a time window
    -- Format: {number}{unit} e.g. '24h', '30d', '2w', '3m', '1y'
    -- NULL for conditions that are pure value thresholds (no time window)
                                        lookback_period         VARCHAR(10),
                                        threshold_value         VARCHAR(255)    NOT NULL,

                                        sys_created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                        sys_updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                                        CONSTRAINT pk_global_rule_conditions    PRIMARY KEY (id),
                                        CONSTRAINT chk_grc_agg  CHECK (aggregation_function IN (
                                                                                                'NONE',   -- pure value threshold
                                                                                                'SUM',    -- sum-based / used as short window tag in SUDDEN_SPIKE
                                                                                                'COUNT',  -- count-based threshold
                                                                                                'AVG',    -- used as long window tag in SUDDEN_SPIKE
                                                                                                'MIN',    -- used as dormant period tag in DORMANT_REACTIVATION
                                                                                                'MAX'     -- used as reactivation window tag in DORMANT_REACTIVATION
                                            )),
                                        CONSTRAINT fk_grc_rule FOREIGN KEY (rule_id) REFERENCES global_rules(id)
);

CREATE TRIGGER trg_global_rule_conditions_updated_at
    BEFORE UPDATE ON global_rule_conditions
    FOR EACH ROW
    EXECUTE FUNCTION update_sys_updated_at_column();

CREATE INDEX idx_global_rule_conditions_rule_id ON global_rule_conditions(rule_id);