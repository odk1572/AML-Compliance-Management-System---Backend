-- Add the reference column to the join table
ALTER TABLE alert_transactions
    ADD COLUMN atx_reference VARCHAR(50);

-- Ensure uniqueness and add an index for fast lookups
ALTER TABLE alert_transactions
    ADD CONSTRAINT uk_atx_reference UNIQUE (atx_reference);

CREATE INDEX idx_atx_ref_lookup ON alert_transactions (atx_reference);