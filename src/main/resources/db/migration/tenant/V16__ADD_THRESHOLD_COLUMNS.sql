ALTER TABLE tenant_rule_thresholds
    ADD COLUMN override_aggregation_function VARCHAR(10);


COMMENT ON COLUMN tenant_rule_thresholds.override_aggregation_function
IS 'Identifies the logic type (SUM, COUNT, NONE) used for this specific threshold override.';

COMMENT ON COLUMN tenant_rule_thresholds.override_lookback_period
IS 'The time window override (e.g., 24h, 7d, 180 days).';