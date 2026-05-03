ALTER TABLE common_schema.global_rule_conditions
    ADD COLUMN aggregation_function VARCHAR(10) NOT NULL DEFAULT 'NONE';

ALTER TABLE common_schema.global_rule_conditions
    ADD COLUMN lookback_period VARCHAR(20);

ALTER TABLE common_schema.global_rule_conditions
    ADD CONSTRAINT chk_aggregation_type
        CHECK (aggregation_function IN ('NONE', 'SUM', 'COUNT', 'AVG', 'MIN', 'MAX'));

COMMENT ON COLUMN common_schema.global_rule_conditions.aggregation_function IS 'Used by the Rule Engine Strategy to determine the SQL aggregation logic.';