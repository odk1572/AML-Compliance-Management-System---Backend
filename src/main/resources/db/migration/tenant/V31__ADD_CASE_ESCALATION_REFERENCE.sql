ALTER TABLE case_escalations
    ADD COLUMN esc_reference VARCHAR(50);

ALTER TABLE case_escalations
    ADD CONSTRAINT uk_esc_reference UNIQUE (esc_reference);

CREATE INDEX idx_esc_ref_lookup ON case_escalations (esc_reference);