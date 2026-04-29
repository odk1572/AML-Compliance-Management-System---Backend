-- Add the condition_reference column
ALTER TABLE common_schema.global_rule_conditions
    ADD COLUMN condition_reference VARCHAR(50);

-- Enforce uniqueness
ALTER TABLE common_schema.global_rule_conditions
    ADD CONSTRAINT uk_grc_reference UNIQUE (condition_reference);

-- Index for fast lookups when the engine evaluates rules
CREATE INDEX idx_grc_ref_lookup ON common_schema.global_rule_conditions (condition_reference);