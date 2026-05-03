ALTER TABLE alert_transactions
    ADD COLUMN atx_reference VARCHAR(50);

ALTER TABLE alert_transactions
    ADD CONSTRAINT uk_atx_reference UNIQUE (atx_reference);

CREATE INDEX idx_atx_ref_lookup ON alert_transactions (atx_reference);