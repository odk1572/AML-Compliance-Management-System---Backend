
SET search_path TO common_schema;

-- ── 1. GLOBAL_WATCHLISTS ──────────────────────────────────────
CREATE TABLE global_watchlists (
                                   id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
                                   list_source         VARCHAR(10)     NOT NULL,
                                   primary_name        VARCHAR(255)    NOT NULL,
                                   entity_type         VARCHAR(15)     NOT NULL DEFAULT 'INDIVIDUAL',

    -- JSONB array of alternate name strings
    -- e.g. ["Jon Doe", "J. Doe", "JD"]
                                   aliases             JSONB           NOT NULL DEFAULT '[]'::JSONB,

    -- Vehicle-specific identifiers
                                   imo_number          VARCHAR(20),        -- International Maritime Organization (vessels)
                                   tail_number         VARCHAR(20),        -- Aircraft registration

                                   country_code        CHAR(2),            -- ISO 3166-1 alpha-2
                                   risk_level          VARCHAR(10)     NOT NULL DEFAULT 'HIGH',
                                   sanction_type       VARCHAR(15)     NOT NULL,
                                   reference_number    VARCHAR(100),       -- Official list reference
                                   listed_on           DATE,
                                   delisted_on         DATE,
                                   is_active           BOOLEAN         NOT NULL DEFAULT TRUE,

    -- Soft delete (entry removed from source, retained for audit)
                                   sys_is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
                                   sys_deleted_at      TIMESTAMPTZ,

    -- Sync tracking
                                   sys_last_synced_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                   sys_created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                   sys_updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                                   CONSTRAINT pk_global_watchlists         PRIMARY KEY (id),
                                   CONSTRAINT chk_gwl_source   CHECK (list_source IN ('OFAC','UN','FATF','EU','LOCAL')),
                                   CONSTRAINT chk_gwl_type     CHECK (entity_type IN ('INDIVIDUAL','ENTITY','VESSEL','AIRCRAFT')),
                                   CONSTRAINT chk_gwl_risk     CHECK (risk_level   IN ('CRITICAL','HIGH','MEDIUM')),
                                   CONSTRAINT chk_gwl_sanction CHECK (sanction_type IN ('SDN','CONSOLIDATED','PEP','SECTORAL','OTHER'))
);

-- FuzzyNameMatcher loads all active entries — this index is critical
CREATE INDEX idx_gwl_active_source
    ON global_watchlists (list_source, is_active)
    WHERE sys_is_deleted = FALSE;

-- GIN index allows efficient JSONB alias search
CREATE INDEX idx_gwl_aliases_gin
    ON global_watchlists USING GIN (aliases);

-- Partial index for fast lookup of specific reference numbers
CREATE INDEX idx_gwl_reference
    ON global_watchlists (reference_number)
    WHERE reference_number IS NOT NULL;

COMMENT ON TABLE  global_watchlists         IS 'Global sanction and PEP entries. Shared across all tenant schemas. FuzzyNameMatcher reads is_active=TRUE entries.';
COMMENT ON COLUMN global_watchlists.aliases IS 'JSONB array of alternate name strings. Included in Jaro-Winkler fuzzy matching.';
COMMENT ON COLUMN global_watchlists.is_active IS 'FALSE when delisted. Row is retained for audit — never hard deleted.';

-- ── 2. WATCHLIST_SYNC_JOBS ────────────────────────────────────
CREATE TABLE watchlist_sync_jobs (
                                     id              UUID            NOT NULL DEFAULT gen_random_uuid(),
                                     list_source     VARCHAR(10)     NOT NULL,
                                     job_status      VARCHAR(10)     NOT NULL DEFAULT 'RUNNING',
                                     records_added   INTEGER         NOT NULL DEFAULT 0,
                                     records_updated INTEGER         NOT NULL DEFAULT 0,
                                     records_removed INTEGER         NOT NULL DEFAULT 0,
                                     error_message   TEXT,
                                     triggered_by    UUID,           -- NULL = scheduled auto-run

                                     started_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                     completed_at    TIMESTAMPTZ,

    -- Immutable job log — no soft delete, status updates only
                                     sys_created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                     sys_updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                                     CONSTRAINT pk_watchlist_sync_jobs       PRIMARY KEY (id),
                                     CONSTRAINT chk_wsj_status   CHECK (job_status IN ('RUNNING','SUCCESS','FAILED')),
                                     CONSTRAINT chk_wsj_source   CHECK (list_source IN ('OFAC','UN','FATF','EU','LOCAL','ALL')),
                                     CONSTRAINT fk_wsj_triggered FOREIGN KEY (triggered_by) REFERENCES platform_users(id)
);

CREATE INDEX idx_wsj_source_status ON watchlist_sync_jobs (list_source, job_status);

COMMENT ON TABLE watchlist_sync_jobs IS 'Immutable log of every watchlist sync run. triggered_by NULL = scheduled cron job.';


CREATE TRIGGER trg_update_global_watchlists_timestamp BEFORE UPDATE ON global_watchlists FOR EACH ROW EXECUTE FUNCTION update_timestamp_column();
CREATE TRIGGER trg_update_watchlist_sync_jobs_timestamp BEFORE UPDATE ON watchlist_sync_jobs FOR EACH ROW EXECUTE FUNCTION update_timestamp_column();