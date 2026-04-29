-- Add the rule_reference column to common_schema
ALTER TABLE common_schema.global_rules
    ADD COLUMN rule_reference VARCHAR(50);

-- Enforce uniqueness to prevent duplicate "labels"
ALTER TABLE common_schema.global_rules
    ADD CONSTRAINT uk_global_rule_reference UNIQUE (rule_reference);

-- Index for lightning-fast searches in the Rule Engine UI
CREATE INDEX idx_global_rule_ref_lookup ON common_schema.global_rules (rule_reference);