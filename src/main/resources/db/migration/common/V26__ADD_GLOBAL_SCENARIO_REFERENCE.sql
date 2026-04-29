-- Add the scenario_reference column to common_schema
ALTER TABLE common_schema.global_scenarios
    ADD COLUMN scenario_reference VARCHAR(50);

-- Enforce uniqueness to prevent duplicate labels
ALTER TABLE common_schema.global_scenarios
    ADD CONSTRAINT uk_global_scenario_reference UNIQUE (scenario_reference);

-- Index for fast scenario lookups and filtering
CREATE INDEX idx_global_scenario_ref_lookup ON common_schema.global_scenarios (scenario_reference);