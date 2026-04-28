-- 1. Add the new columns as nullable first
ALTER TABLE str_filings ADD COLUMN rule_type VARCHAR(100);
ALTER TABLE str_filings ADD COLUMN typology_triggered VARCHAR(255);

-- 2. Backfill the data from the linked cases table
-- This ensures your old STRs inherit the correct forensic data
UPDATE str_filings s
SET rule_type = c.rule_type,
    typology_triggered = c.typology_triggered
    FROM cases c
WHERE s.case_id = c.id;

-- 3. Fallback: If any cases had null values, provide a default so the NOT NULL constraint doesn't fail
UPDATE str_filings SET rule_type = 'MIGRATED' WHERE rule_type IS NULL;
UPDATE str_filings SET typology_triggered = 'MIGRATED' WHERE typology_triggered IS NULL;

-- 4. Now that there are no NULLs, enforce the NOT NULL constraint
ALTER TABLE str_filings ALTER COLUMN rule_type SET NOT NULL;
ALTER TABLE str_filings ALTER COLUMN typology_triggered SET NOT NULL;

-- 5. Drop the old category column as requested
ALTER TABLE str_filings DROP COLUMN IF EXISTS typology_category;