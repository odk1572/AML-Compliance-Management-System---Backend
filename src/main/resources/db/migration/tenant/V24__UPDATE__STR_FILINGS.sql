-- Add the new relation
ALTER TABLE str_filings
    ADD COLUMN customer_profile_id UUID REFERENCES customer_profiles(id) ON DELETE RESTRICT;

-- Drop the old hardcoded strings
ALTER TABLE str_filings
DROP COLUMN subject_name,
DROP COLUMN subject_account_no;

CREATE INDEX idx_str_filings_customer_profile_id ON str_filings(customer_profile_id);