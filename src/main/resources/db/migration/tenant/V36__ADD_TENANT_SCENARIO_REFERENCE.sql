ALTER TABLE tenant_scenarios
    ADD COLUMN tsc_reference VARCHAR(50);

ALTER TABLE tenant_scenarios
    ADD CONSTRAINT uk_tsc_reference UNIQUE (tsc_reference);

CREATE INDEX idx_tsc_ref_lookup ON tenant_scenarios (tsc_reference);