ALTER TABLE str_filings ADD COLUMN rule_type VARCHAR(100);
ALTER TABLE str_filings ADD COLUMN typology_triggered VARCHAR(255);

UPDATE str_filings s
SET rule_type = c.rule_type,
    typology_triggered = c.typology_triggered
    FROM cases c
WHERE s.case_id = c.id;

UPDATE str_filings SET rule_type = 'MIGRATED' WHERE rule_type IS NULL;
UPDATE str_filings SET typology_triggered = 'MIGRATED' WHERE typology_triggered IS NULL;

ALTER TABLE str_filings ALTER COLUMN rule_type SET NOT NULL;
ALTER TABLE str_filings ALTER COLUMN typology_triggered SET NOT NULL;

ALTER TABLE str_filings DROP COLUMN IF EXISTS typology_category;