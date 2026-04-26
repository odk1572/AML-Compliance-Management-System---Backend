-- 1. Add aggregation_function with a default value to satisfy existing records
ALTER TABLE common_schema.global_rule_conditions
    ADD COLUMN aggregation_function VARCHAR(10) NOT NULL DEFAULT 'NONE';

-- 2. Add lookback_period for time-window based rules (Velocity/Structuring)
ALTER TABLE common_schema.global_rule_conditions
    ADD COLUMN lookback_period VARCHAR(20);

-- 3. Add a check constraint to ensure only supported functions are stored
-- This prevents the Rule Engine from crashing on unknown strings
ALTER TABLE common_schema.global_rule_conditions
    ADD CONSTRAINT chk_aggregation_type
        CHECK (aggregation_function IN ('NONE', 'SUM', 'COUNT', 'AVG', 'MIN', 'MAX'));

-- 4. Update the audit columns (if your seeder didn't handle them)
-- This ensures that your 'sys_updated_at' logic remains consistent
COMMENT ON COLUMN common_schema.global_rule_conditions.aggregation_function IS 'Used by the Rule Engine Strategy to determine the SQL aggregation logic.';