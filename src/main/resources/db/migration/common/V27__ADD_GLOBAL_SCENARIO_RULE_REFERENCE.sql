-- Add the gsr_reference column
ALTER TABLE common_schema.global_scenario_rules
    ADD COLUMN gsr_reference VARCHAR(50);

-- Enforce uniqueness
ALTER TABLE common_schema.global_scenario_rules
    ADD CONSTRAINT uk_gsr_reference UNIQUE (gsr_reference);

-- Index for fast lookup when managing scenario configurations
CREATE INDEX idx_gsr_ref_lookup ON common_schema.global_scenario_rules (gsr_reference);