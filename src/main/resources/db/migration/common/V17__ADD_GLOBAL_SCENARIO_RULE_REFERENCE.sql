ALTER TABLE common_schema.global_scenario_rules
    ADD COLUMN gsr_reference VARCHAR(50);

ALTER TABLE common_schema.global_scenario_rules
    ADD CONSTRAINT uk_gsr_reference UNIQUE (gsr_reference);

CREATE INDEX idx_gsr_ref_lookup ON common_schema.global_scenario_rules (gsr_reference);