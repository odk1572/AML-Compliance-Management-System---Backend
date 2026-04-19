
SET search_path TO common_schema;

CREATE TABLE geographic_risk_ratings (
    id                      UUID        NOT NULL DEFAULT gen_random_uuid(),
    country_code            CHAR(2)     NOT NULL,   -- ISO 3166-1 alpha-2
    country_name            VARCHAR(100) NOT NULL,
    fatf_status             VARCHAR(15) NOT NULL DEFAULT 'COMPLIANT',
    -- Basel AML Index 0 (low risk) to 10 (highest risk)
    basel_aml_index_score   NUMERIC(4,2) NOT NULL DEFAULT 0.00
                                CHECK (basel_aml_index_score BETWEEN 0 AND 10),
    risk_tier               VARCHAR(10) NOT NULL DEFAULT 'LOW',
    notes                   TEXT,

    -- Soft delete
    sys_is_deleted          BOOLEAN     NOT NULL DEFAULT FALSE,
    sys_deleted_at          TIMESTAMPTZ,

    -- When this rating version became effective
    effective_from          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sys_created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sys_updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_geographic_risk_ratings       PRIMARY KEY (id),
    CONSTRAINT uq_geo_risk_country              UNIQUE (country_code),
    CONSTRAINT chk_geo_risk_fatf    CHECK (fatf_status IN ('BLACKLIST','GREYLIST','COMPLIANT')),
    CONSTRAINT chk_geo_risk_tier    CHECK (risk_tier   IN ('CRITICAL','HIGH','MEDIUM','LOW'))
);

CREATE INDEX idx_geo_risk_tier
    ON geographic_risk_ratings (risk_tier, fatf_status)
    WHERE sys_is_deleted = FALSE;

-- Seed FATF blacklist countries (as of common knowledge — update via admin UI)
INSERT INTO geographic_risk_ratings
    (country_code, country_name, fatf_status, basel_aml_index_score, risk_tier, notes)
VALUES
    ('KP', 'North Korea',   'BLACKLIST', 9.50, 'CRITICAL', 'FATF Call for Action — highest risk'),
    ('IR', 'Iran',          'BLACKLIST', 8.20, 'CRITICAL', 'FATF Call for Action'),
    ('MM', 'Myanmar',       'GREYLIST',  7.10, 'HIGH',     'FATF Increased Monitoring'),
    ('SY', 'Syria',         'GREYLIST',  7.80, 'HIGH',     'FATF Increased Monitoring'),
    ('YE', 'Yemen',         'GREYLIST',  7.50, 'HIGH',     'FATF Increased Monitoring'),
    ('PK', 'Pakistan',      'GREYLIST',  6.80, 'HIGH',     'FATF Increased Monitoring'),
    ('NG', 'Nigeria',       'GREYLIST',  6.40, 'HIGH',     'FATF Increased Monitoring'),
    ('PH', 'Philippines',   'GREYLIST',  5.90, 'HIGH',     'FATF Increased Monitoring'),
    ('VN', 'Vietnam',       'GREYLIST',  5.70, 'MEDIUM',   'FATF Increased Monitoring'),
    ('US', 'United States', 'COMPLIANT', 5.10, 'MEDIUM',   'FinCEN jurisdiction'),
    ('GB', 'United Kingdom','COMPLIANT', 4.90, 'MEDIUM',   'FCA jurisdiction'),
    ('IN', 'India',         'COMPLIANT', 5.30, 'MEDIUM',   'FIU-IND jurisdiction'),
    ('DE', 'Germany',       'COMPLIANT', 4.20, 'LOW',      'EU jurisdiction'),
    ('SG', 'Singapore',     'COMPLIANT', 4.50, 'LOW',      'MAS jurisdiction'),
    ('AE', 'UAE',           'COMPLIANT', 5.60, 'MEDIUM',   'CBUAE jurisdiction');

COMMENT ON TABLE  geographic_risk_ratings IS 'Basel AML Index + FATF status per country. Used by GEO_RISK rule conditions. Seeded with FATF lists — admin updates via UI.';
COMMENT ON COLUMN geographic_risk_ratings.effective_from IS 'When this version became effective. New row inserted per rating update for historical accuracy.';