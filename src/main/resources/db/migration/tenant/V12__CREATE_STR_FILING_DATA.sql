CREATE TABLE str_filings (
                             id UUID PRIMARY KEY,
                             case_id UUID NOT NULL REFERENCES cases(id) ON DELETE RESTRICT,

                             filing_reference VARCHAR(50) UNIQUE NOT NULL, -- Format: STR-YYYYMMDD-XXXXX
                             regulatory_body VARCHAR(50) NOT NULL,        -- e.g., FinCEN / FIU_IND
                             typology_category VARCHAR(100) NOT NULL,     -- e.g., STRUCTURING / LAYERING

                             subject_name VARCHAR(255) NOT NULL,
                             subject_account_no VARCHAR(50) NOT NULL,

                             suspicion_narrative TEXT NOT NULL,
                             filed_by UUID NOT NULL REFERENCES tenant_users(id) ON DELETE RESTRICT,

    -- Immutable legal record
                             sys_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indices for regulatory audits and reporting
CREATE INDEX idx_str_case_id ON str_filings(case_id);
CREATE INDEX idx_str_filed_by ON str_filings(filed_by);
CREATE INDEX idx_str_regulatory_body ON str_filings(regulatory_body);
CREATE INDEX idx_str_reference ON str_filings(filing_reference);
CREATE INDEX idx_str_created_at ON str_filings(sys_created_at);

CREATE TABLE str_filing_transactions (
                                         id UUID PRIMARY KEY,
                                         str_filing_id UUID NOT NULL REFERENCES str_filings(id) ON DELETE CASCADE,
                                         transaction_id UUID NOT NULL REFERENCES transactions(id) ON DELETE RESTRICT,

    -- Immutable record
                                         sys_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Prevent duplicate linking of the same transaction to the same report
                                         CONSTRAINT uk_str_transaction UNIQUE (str_filing_id, transaction_id)
);

CREATE INDEX idx_sft_filing_id ON str_filing_transactions(str_filing_id);
CREATE INDEX idx_sft_transaction_id ON str_filing_transactions(transaction_id);