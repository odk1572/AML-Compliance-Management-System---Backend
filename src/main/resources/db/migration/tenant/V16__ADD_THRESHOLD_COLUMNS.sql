-- ============================================================
-- MIGRATION: Fix column mismatch in tenant_rule_thresholds
-- ============================================================

-- 1. Add override_aggregation_function
-- Even if most tenants don't change this, the Java Strategy
-- pattern requires it to map the Condition to the Switch/Case logic.
ALTER TABLE tenant_rule_thresholds
    ADD COLUMN override_aggregation_function VARCHAR(10);


-- 3. Update Audit Trail (Optional but Recommended)
-- If you want to track which strategy was used at the time of an alert
-- the JSONB columns (prev_state/next_state) in tenant_audit_log
-- handle this automatically, so no physical column change is needed there.

-- 4. Documentation
COMMENT ON COLUMN tenant_rule_thresholds.override_aggregation_function
IS 'Identifies the logic type (SUM, COUNT, NONE) used for this specific threshold override.';

COMMENT ON COLUMN tenant_rule_thresholds.override_lookback_period
IS 'The time window override (e.g., 24h, 7d, 180 days).';