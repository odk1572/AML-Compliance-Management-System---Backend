-- ── 1. STR_FILINGS ────────────────────────────────────────────
CREATE TABLE str_filings (
    id                      UUID            NOT NULL DEFAULT gen_random_uuid(),

    -- Parent case — one STR per case maximum
    case_id                 UUID            NOT NULL,

    -- Human-readable reference: STR-YYYYMMDD-XXXXX
    filing_reference        VARCHAR(25)     NOT NULL,

    -- Jurisdiction of the filing
    regulatory_body         VARCHAR(15)     NOT NULL,

    -- FATF/FIU typology classification — required for regulatory reports
    -- Per DB design suggestion: added for STR report filtering
    typology_category       VARCHAR(20)     NOT NULL,

    -- Subject details (pre-populated from CustomerProfile by StrFilingService)
    subject_name            VARCHAR(255)    NOT NULL,
    subject_account_no      VARCHAR(50)     NOT NULL,

    -- CO-authored mandatory narrative sections
    suspicion_narrative     TEXT            NOT NULL,

    -- Generated document URLs (Cloudinary)
    pdf_cloudinary_url      TEXT            NOT NULL,   -- Thymeleaf → OpenPDF output
    xml_cloudinary_url      TEXT,                       -- JAXB FIU-IND XML (nullable for FinCEN)

    -- Who filed (Compliance Officer)
    filed_by                UUID            NOT NULL,

    -- Single immutable timestamp — no updates ever
    sys_created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- ── Constraints ──────────────────────────────────────────
    CONSTRAINT pk_str_filings
        PRIMARY KEY (id),

    CONSTRAINT uq_str_filings_reference
        UNIQUE (filing_reference),

    -- One STR per case — hard regulatory rule
    CONSTRAINT uq_str_filings_case
        UNIQUE (case_id),

    CONSTRAINT chk_str_regulatory_body
        CHECK (regulatory_body IN (
            'FinCEN',       -- US Financial Crimes Enforcement Network
            'FIU_IND',      -- India Financial Intelligence Unit
            'FINTRAC',      -- Canada
            'AUSTRAC',      -- Australia
            'OTHER'
        )),

    CONSTRAINT chk_str_typology_category
        CHECK (typology_category IN (
            'STRUCTURING',
            'LAYERING',
            'PEP',
            'FRAUD_ML',
            'GEO_RISK',
            'VELOCITY',
            'ROUND_AMOUNT',
            'DORMANCY',
            'OTHER'
        )),

    CONSTRAINT chk_str_narrative_length
        CHECK (length(trim(suspicion_narrative)) >= 20),

    CONSTRAINT fk_str_case
        FOREIGN KEY (case_id)
        REFERENCES cases (id),

    CONSTRAINT fk_str_filed_by
        FOREIGN KEY (filed_by)
        REFERENCES tenant_users (id)
);

-- SAR/STR Filing Log report — ordered by filing date
CREATE INDEX idx_str_filed_time
    ON str_filings (filed_by, sys_created_at DESC);

-- Typology distribution report (cross-tenant + tenant level)
CREATE INDEX idx_str_typology
    ON str_filings (typology_category, sys_created_at DESC);

-- Regulatory body filter
CREATE INDEX idx_str_regulatory_body
    ON str_filings (regulatory_body, sys_created_at DESC);

-- ── IMMUTABILITY ENFORCEMENT ──────────────────────────────────
-- Filed SARs/STRs are permanent regulatory records.
-- Cannot be deleted or modified — SRS §4.4.
CREATE OR REPLACE RULE no_update_str_filings AS
    ON UPDATE TO str_filings
    DO INSTEAD NOTHING;

CREATE OR REPLACE RULE no_delete_str_filings AS
    ON DELETE TO str_filings
    DO INSTEAD NOTHING;


-- ── 2. STR_FILING_TRANSACTIONS ────────────────────────────────
CREATE TABLE str_filing_transactions (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    str_filing_id   UUID        NOT NULL,
    transaction_id  UUID        NOT NULL,

    -- Immutable junction
    sys_created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- ── Constraints ──────────────────────────────────────────
    CONSTRAINT pk_str_filing_transactions
        PRIMARY KEY (id),

    CONSTRAINT uq_sft_pair
        UNIQUE (str_filing_id, transaction_id),

    CONSTRAINT fk_sft_filing
        FOREIGN KEY (str_filing_id)
        REFERENCES str_filings (id),

    CONSTRAINT fk_sft_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES transactions (id)
);

-- Load all transactions for a given STR filing (document generation)
CREATE INDEX idx_sft_filing
    ON str_filing_transactions (str_filing_id);

-- Cross-reference: which STR(s) include a given transaction
CREATE INDEX idx_sft_transaction
    ON str_filing_transactions (transaction_id);

CREATE OR REPLACE RULE no_delete_str_filing_transactions AS
    ON DELETE TO str_filing_transactions
    DO INSTEAD NOTHING;
