ALTER TABLE case_transactions
    ADD COLUMN ctx_reference VARCHAR(50);

ALTER TABLE case_transactions
    ADD CONSTRAINT uk_ctx_reference UNIQUE (ctx_reference);

CREATE INDEX idx_ctx_ref_lookup ON case_transactions (ctx_reference);