
CREATE TABLE customer_profiles (
    id                          UUID            NOT NULL DEFAULT gen_random_uuid(),

    -- Primary business identifier
    account_number              VARCHAR(50)     NOT NULL,

    -- Customer identity
    customer_name               VARCHAR(255)    NOT NULL,
    customer_type               VARCHAR(15)     NOT NULL DEFAULT 'INDIVIDUAL',
    id_type                     VARCHAR(30),    -- PASSPORT | NID | DRIVING_LICENCE | TIN
    id_number                   VARCHAR(50),
    nationality                 CHAR(2),        -- ISO 3166-1 alpha-2
    country_of_residence        CHAR(2),        -- ISO 3166-1 alpha-2

    -- Risk profile (maintained by risk scoring logic)
    risk_rating                 VARCHAR(10)     NOT NULL DEFAULT 'LOW',
    risk_score                  SMALLINT        NOT NULL DEFAULT 0
                                    CHECK (risk_score BETWEEN 0 AND 100),

    -- Risk flags (set by ingestion pipeline + watchlist screening)
    is_pep                      BOOLEAN         NOT NULL DEFAULT FALSE,
    is_sanctioned               BOOLEAN         NOT NULL DEFAULT FALSE,
    is_dormant                  BOOLEAN         NOT NULL DEFAULT FALSE,

    -- Cross-schema ref: common_schema.global_watchlists.id
    -- NULLABLE — set only when is_sanctioned=TRUE or is_pep=TRUE
    -- Gives CO a direct link to the matched watchlist entry in 360° view
    -- No FK constraint (cross-schema) — enforced at service layer
    matched_watchlist_id        UUID,

    -- Account history
    account_opened_on           DATE,
    last_activity_date          DATE,

    -- KYC status
    kyc_status                  VARCHAR(10)     NOT NULL DEFAULT 'PENDING',

    -- Cloudinary KYC document identifiers
    -- Dynamic watermarking applied via CloudinaryService.getSignedUrl()
    kyc_doc_cloudinary_public_id    VARCHAR(255),
    kyc_doc_cloudinary_url          TEXT,

    -- Soft delete (5-year regulatory retention)
    sys_is_deleted              BOOLEAN         NOT NULL DEFAULT FALSE,
    sys_deleted_at              TIMESTAMPTZ,

    -- Audit timestamps
    sys_created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    sys_updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- ── Constraints ──────────────────────────────────────────
    CONSTRAINT pk_customer_profiles
        PRIMARY KEY (id),

    CONSTRAINT uq_customer_profiles_account
        UNIQUE (account_number),

    CONSTRAINT chk_cp_customer_type
        CHECK (customer_type IN ('INDIVIDUAL', 'CORPORATE', 'PEP')),

    CONSTRAINT chk_cp_risk_rating
        CHECK (risk_rating IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),

    CONSTRAINT chk_cp_kyc_status
        CHECK (kyc_status IN ('VERIFIED', 'PENDING', 'EXPIRED')),

    -- Sanction flag must align: if is_sanctioned=TRUE, matched_watchlist_id should be set
    -- Enforced as a soft business rule (WARN not block) — NULL allowed for legacy data
    CONSTRAINT chk_cp_dormant_activity
        CHECK (
            is_dormant = FALSE OR
            last_activity_date IS NOT NULL
        )
);

-- ── Indexes ───────────────────────────────────────────────────
-- Primary lookup — ingestion pipeline upserts by account_number
CREATE UNIQUE INDEX idx_cp_account_number_active
    ON customer_profiles (account_number)
    WHERE sys_is_deleted = FALSE;

-- CO investigation: filter high-risk / PEP / sanctioned customers
CREATE INDEX idx_cp_risk_flags
    ON customer_profiles (risk_rating, is_pep, is_sanctioned)
    WHERE sys_is_deleted = FALSE;

-- Watchlist match lookup — "show all customers matched to this watchlist entry"
CREATE INDEX idx_cp_watchlist_match
    ON customer_profiles (matched_watchlist_id)
    WHERE matched_watchlist_id IS NOT NULL
    AND sys_is_deleted = FALSE;

-- Dormant account monitoring
CREATE INDEX idx_cp_dormant
    ON customer_profiles (is_dormant, last_activity_date)
    WHERE is_dormant = TRUE AND sys_is_deleted = FALSE;
