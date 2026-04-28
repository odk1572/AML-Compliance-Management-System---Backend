-- 1. Add the new rule_type column.
-- We use a temporary DEFAULT so that any existing rows in your database don't violate the NOT NULL constraint.
ALTER TABLE alerts
    ADD COLUMN rule_type VARCHAR(100) NOT NULL DEFAULT 'LEGACY_RULE';

-- Remove the default constraint immediately after so future inserts are forced to provide it
ALTER TABLE alerts
    ALTER COLUMN rule_type DROP DEFAULT;

-- 3. Create an index for the new rule_type column to make dashboard filtering faster
CREATE INDEX idx_alerts_rule_type ON alerts(rule_type);