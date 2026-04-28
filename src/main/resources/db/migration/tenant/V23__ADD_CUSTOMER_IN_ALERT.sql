-- Add the column (allowing NULL initially for existing records)
ALTER TABLE cases
    ADD COLUMN customer_profile_id UUID REFERENCES customer_profiles(id) ON DELETE RESTRICT;

-- Index it since you will definitely query "Show me all cases for Customer X"
CREATE INDEX idx_cases_customer_profile_id ON cases(customer_profile_id);

-- Optional: If you want to enforce NOT NULL, you would need to run an UPDATE script
-- to backfill the customer_profile_id for existing cases based on their linked alerts.