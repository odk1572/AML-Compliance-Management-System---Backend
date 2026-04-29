-- Add the reference column
ALTER TABLE alert_evidence
    ADD COLUMN evidence_reference VARCHAR(50);

-- Make it unique and indexed for fast lookups
ALTER TABLE alert_evidence
    ADD CONSTRAINT uk_evidence_reference UNIQUE (evidence_reference);

CREATE INDEX idx_evidence_ref_lookup ON alert_evidence (evidence_reference);