ALTER TABLE case_audit_trail
    ADD COLUMN cat_reference VARCHAR(50);

ALTER TABLE case_audit_trail
    ADD CONSTRAINT uk_cat_reference UNIQUE (cat_reference);

CREATE INDEX idx_cat_ref_lookup ON case_audit_trail (cat_reference);