
CREATE TABLE transactions (
                              id                      UUID            NOT NULL DEFAULT gen_random_uuid(),

    -- Batch this transaction was ingested from
                              batch_id                UUID            NOT NULL,

    -- Originator customer profile link
                              customer_id             UUID            NOT NULL,

    -- Source system unique identifier — dedup key within a batch
                              transaction_ref         VARCHAR(100)    NOT NULL,

    -- ── Originator ───────────────────────────────────────────
                              originator_account_no   VARCHAR(50)     NOT NULL,
                              originator_name         VARCHAR(255)    NOT NULL,
                              originator_bank_code    VARCHAR(20),
                              originator_country      CHAR(2),        -- ISO 3166-1 alpha-2

    -- ── Beneficiary ──────────────────────────────────────────
                              beneficiary_account_no  VARCHAR(50)     NOT NULL,
                              beneficiary_name        VARCHAR(255)    NOT NULL,
                              beneficiary_bank_code   VARCHAR(20),
                              beneficiary_country     CHAR(2),        -- ISO 3166-1 alpha-2

    -- ── Financial details ────────────────────────────────────
                              amount                  NUMERIC(20, 4)  NOT NULL
                                  CHECK (amount > 0),
                              currency_code           CHAR(3)         NOT NULL,   -- ISO 4217

                              transaction_type        VARCHAR(15)     NOT NULL,
                              channel                 VARCHAR(10)     NOT NULL,
                              transaction_timestamp   TIMESTAMPTZ     NOT NULL,
                              reference_note          VARCHAR(500),

    -- Processing status — updated by RuleEngineStep after screening
                              status                  VARCHAR(15)     NOT NULL DEFAULT 'CLEAN',

    -- ── Single immutable timestamp ────────────────────────────
    -- No sys_updated_at. No sys_is_deleted. Intentional.
                              sys_created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- ── Constraints ──────────────────────────────────────────
                              CONSTRAINT pk_transactions
                                  PRIMARY KEY (id),

    -- Global dedup: same transaction_ref cannot appear twice
                              CONSTRAINT uq_transactions_ref
                                  UNIQUE (transaction_ref),

                              CONSTRAINT chk_txn_transaction_type
                                  CHECK (transaction_type IN (
                                                              'WIRE', 'SWIFT', 'ACH', 'INTERNAL',
                                                              'CASH', 'RTGS', 'NEFT', 'IMPS', 'OTHER'
                                      )),

                              CONSTRAINT chk_txn_channel
                                  CHECK (channel IN ('ONLINE', 'BRANCH', 'ATM', 'API', 'MOBILE')),

                              CONSTRAINT chk_txn_status
                                  CHECK (status IN ('CLEAN', 'FLAGGED', 'UNDER_REVIEW')),

                              CONSTRAINT chk_txn_currency_code
                                  CHECK (currency_code ~ '^[A-Z]{3}$'),

    CONSTRAINT chk_txn_country_orig
        CHECK (originator_country IS NULL OR originator_country ~ '^[A-Z]{2}$'),

    CONSTRAINT chk_txn_country_bene
        CHECK (beneficiary_country IS NULL OR beneficiary_country ~ '^[A-Z]{2}$'),

    CONSTRAINT fk_txn_batch
        FOREIGN KEY (batch_id)
        REFERENCES transaction_batches (id),

    CONSTRAINT fk_txn_customer
        FOREIGN KEY (customer_id)
        REFERENCES customer_profiles (id)
);

-- ── Indexes ───────────────────────────────────────────────────
-- CO transaction history panel — most frequent query in investigation view
-- Orders by time DESC so most recent transactions appear first
CREATE INDEX idx_txn_customer_time
    ON transactions (customer_id, transaction_timestamp DESC);

-- Linked account discovery — CustomerInvestigationService.getLinkedAccounts()
-- Finds accounts sharing originator_name across transactions
CREATE INDEX idx_txn_originator_name
    ON transactions (originator_name, transaction_timestamp DESC);

-- Linked account discovery — beneficiary side
CREATE INDEX idx_txn_beneficiary_name
    ON transactions (beneficiary_name);

-- Account number lookups (360° view transaction history)
CREATE INDEX idx_txn_originator_account
    ON transactions (originator_account_no, transaction_timestamp DESC);

CREATE INDEX idx_txn_beneficiary_account
    ON transactions (beneficiary_account_no);

-- Batch contents query (BatchProcessingReport)
CREATE INDEX idx_txn_batch
    ON transactions (batch_id);

-- Geographic risk reporting — transactions by country
CREATE INDEX idx_txn_countries
    ON transactions (originator_country, beneficiary_country);

-- Amount-range queries (rule engine lookback window aggregations)
CREATE INDEX idx_txn_amount_time
    ON transactions (amount, transaction_timestamp DESC);

-- ── IMMUTABILITY ENFORCEMENT ──────────────────────────────────
-- Transactions are NEVER modified or deleted — regulatory requirement.
-- These RULEs enforce immutability at the PostgreSQL storage layer,
-- below the application layer. Even direct DB admin connections
-- cannot mutate transaction rows.

CREATE OR REPLACE RULE no_update_transactions AS
    ON UPDATE TO transactions
                  DO INSTEAD NOTHING;

CREATE OR REPLACE RULE no_delete_transactions AS
    ON DELETE TO transactions
    DO INSTEAD NOTHING;
