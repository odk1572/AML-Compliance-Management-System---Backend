-- Add the trl_reference column to the tenant schema
ALTER TABLE tenant_rules
    ADD COLUMN trl_reference VARCHAR(50);

-- Ensure uniqueness and add an index for lookups
ALTER TABLE tenant_rules
    ADD CONSTRAINT uk_trl_reference UNIQUE (trl_reference);

CREATE INDEX idx_trl_ref_lookup ON tenant_rules (trl_reference);