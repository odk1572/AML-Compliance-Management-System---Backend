ALTER TABLE str_filing_transactions
    ADD COLUMN sft_reference VARCHAR(50);

ALTER TABLE str_filing_transactions
    ADD CONSTRAINT uk_sft_reference UNIQUE (sft_reference);

CREATE INDEX idx_sft_ref_lookup ON str_filing_transactions (sft_reference);