ALTER TABLE cases
    ADD COLUMN rule_type VARCHAR(100) NOT NULL DEFAULT 'MIXED_ALERTS',
ADD COLUMN typology_triggered VARCHAR(255) NOT NULL DEFAULT 'Multiple Typologies';

ALTER TABLE cases
    ALTER COLUMN rule_type DROP DEFAULT,
ALTER COLUMN typology_triggered DROP DEFAULT;

CREATE INDEX idx_cases_rule_type ON cases(rule_type);
CREATE INDEX idx_cases_typology ON cases(typology_triggered);