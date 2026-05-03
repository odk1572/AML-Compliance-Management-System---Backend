ALTER TABLE cases
    ADD COLUMN customer_profile_id UUID REFERENCES customer_profiles(id) ON DELETE RESTRICT;

CREATE INDEX idx_cases_customer_profile_id ON cases(customer_profile_id);
