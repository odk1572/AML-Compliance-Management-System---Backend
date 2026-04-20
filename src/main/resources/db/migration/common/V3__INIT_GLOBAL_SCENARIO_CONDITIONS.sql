

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
                                        id UUID PRIMARY KEY,
                                        rule_id UUID NOT NULL REFERENCES global_rules(id) ON DELETE CASCADE,
                                        attribute_name VARCHAR(100) NOT NULL,
                                        condition_sequence INT NOT NULL,
                                        aggregation_function VARCHAR(50) NOT NULL DEFAULT 'NONE',
                                        lookback_period VARCHAR(50),
                                        operator VARCHAR(50) NOT NULL,
                                        threshold_value VARCHAR(255) NOT NULL,
                                        value_data_type VARCHAR(50) NOT NULL,
                                        sys_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        sys_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_global_rule_conditions_updated_at
    BEFORE UPDATE ON global_rule_conditions
    FOR EACH ROW
    EXECUTE FUNCTION update_sys_updated_at_column();

CREATE INDEX idx_global_rule_conditions_rule_id ON global_rule_conditions(rule_id);