
CREATE TABLE transactions (
                              id UUID PRIMARY KEY,
                              batch_id UUID NOT NULL REFERENCES transaction_batches(id),
                              customer_id UUID REFERENCES customer_profiles(id) ON DELETE SET NULL,
                              transaction_ref VARCHAR(100) UNIQUE NOT NULL,

    -- Originator Details
                              originator_account_no VARCHAR(50),
                              originator_name VARCHAR(255),
                              originator_bank_code VARCHAR(50),
                              originator_country VARCHAR(3), -- ISO Country Code

    -- Beneficiary Details
                              beneficiary_account_no VARCHAR(50),
                              beneficiary_name VARCHAR(255),
                              beneficiary_bank_code VARCHAR(50),
                              beneficiary_country VARCHAR(3), -- ISO Country Code

    -- Financial Data
                              amount DECIMAL(20, 2) NOT NULL,
                              currency_code VARCHAR(3) NOT NULL,
                              transaction_type VARCHAR(20) NOT NULL, -- WIRE / SWIFT / ACH / INTERNAL / CASH
                              channel VARCHAR(20) NOT NULL, -- ONLINE / BRANCH / ATM / UPI

    -- Timestamps
                              transaction_timestamp TIMESTAMP NOT NULL,
                              reference_note TEXT,

    -- Status Tracking
                              status VARCHAR(20) NOT NULL DEFAULT 'CLEAN', -- CLEAN / FLAGGED / UNDER_REVIEW

                              sys_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE INDEX idx_transactions_batch_id ON transactions(batch_id);

CREATE INDEX idx_transactions_customer_id ON transactions(customer_id);

CREATE INDEX idx_transactions_timestamp ON transactions(transaction_timestamp);

CREATE INDEX idx_transactions_countries ON transactions(originator_country, beneficiary_country);

CREATE INDEX idx_transactions_status ON transactions(status);

CREATE INDEX idx_transactions_ref ON transactions(transaction_ref);

CREATE INDEX idx_transactions_amount ON transactions(amount);