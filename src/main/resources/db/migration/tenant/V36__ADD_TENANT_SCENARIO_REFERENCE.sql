-- Add the tsc_reference column to the tenant schema
ALTER TABLE tenant_scenarios
    ADD COLUMN tsc_reference VARCHAR(50);

-- Ensure uniqueness for each activation record
ALTER TABLE tenant_scenarios
    ADD CONSTRAINT uk_tsc_reference UNIQUE (tsc_reference);

-- Index for fast lookup in the "My Scenarios" dashboard
CREATE INDEX idx_tsc_ref_lookup ON tenant_scenarios (tsc_reference);