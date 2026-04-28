-- 1. Add the new columns with a temporary default for existing records
ALTER TABLE cases
    ADD COLUMN rule_type VARCHAR(100) NOT NULL DEFAULT 'MIXED_ALERTS',
ADD COLUMN typology_triggered VARCHAR(255) NOT NULL DEFAULT 'Multiple Typologies';

-- 2. Drop the defaults immediately so new inserts require explicit values from your Java app
ALTER TABLE cases
    ALTER COLUMN rule_type DROP DEFAULT,
ALTER COLUMN typology_triggered DROP DEFAULT;

-- 3. Add indices. Since the main reason we added these to the Case level
-- is to filter the "My Cases" dashboard, indices are highly recommended here.
CREATE INDEX idx_cases_rule_type ON cases(rule_type);
CREATE INDEX idx_cases_typology ON cases(typology_triggered);