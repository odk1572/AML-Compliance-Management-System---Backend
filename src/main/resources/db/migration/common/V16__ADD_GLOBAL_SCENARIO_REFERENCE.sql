ALTER TABLE common_schema.global_scenarios
    ADD COLUMN scenario_reference VARCHAR(50);

ALTER TABLE common_schema.global_scenarios
    ADD CONSTRAINT uk_global_scenario_reference UNIQUE (scenario_reference);

CREATE INDEX idx_global_scenario_ref_lookup ON common_schema.global_scenarios (scenario_reference);