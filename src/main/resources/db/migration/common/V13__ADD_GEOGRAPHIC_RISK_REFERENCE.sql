-- Add the reference column to common_schema
ALTER TABLE common_schema.geographic_risk_ratings
    ADD COLUMN risk_rating_ref VARCHAR(50);

-- Ensure uniqueness and add an index
ALTER TABLE common_schema.geographic_risk_ratings
    ADD CONSTRAINT uk_grr_reference UNIQUE (risk_rating_ref);

CREATE INDEX idx_grr_ref_lookup ON common_schema.geographic_risk_ratings (risk_rating_ref);