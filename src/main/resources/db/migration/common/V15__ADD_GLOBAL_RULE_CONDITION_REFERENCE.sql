ALTER TABLE common_schema.global_rule_conditions
    ADD COLUMN condition_reference VARCHAR(50);

ALTER TABLE common_schema.global_rule_conditions
    ADD CONSTRAINT uk_grc_reference UNIQUE (condition_reference);

CREATE INDEX idx_grc_ref_lookup ON common_schema.global_rule_conditions (condition_reference);