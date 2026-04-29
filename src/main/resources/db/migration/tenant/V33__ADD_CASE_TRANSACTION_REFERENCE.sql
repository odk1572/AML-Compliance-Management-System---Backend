-- Add the reference column to the join table
ALTER TABLE case_transactions
    ADD COLUMN ctx_reference VARCHAR(50);

-- Ensure uniqueness and add an index for fast lookups
ALTER TABLE case_transactions
    ADD CONSTRAINT uk_ctx_reference UNIQUE (ctx_reference);

CREATE INDEX idx_ctx_ref_lookup ON case_transactions (ctx_reference);