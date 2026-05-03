ALTER TABLE alerts
    ADD COLUMN rule_type VARCHAR(100) NOT NULL DEFAULT 'LEGACY_RULE';

ALTER TABLE alerts
    ALTER COLUMN rule_type DROP DEFAULT;

CREATE INDEX idx_alerts_rule_type ON alerts(rule_type);