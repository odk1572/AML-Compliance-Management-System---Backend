ALTER TABLE common_schema.global_rules
    ADD COLUMN rule_reference VARCHAR(50);

ALTER TABLE common_schema.global_rules
    ADD CONSTRAINT uk_global_rule_reference UNIQUE (rule_reference);

CREATE INDEX idx_global_rule_ref_lookup ON common_schema.global_rules (rule_reference);