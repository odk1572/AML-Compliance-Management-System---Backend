

SET search_path TO common_schema;

CREATE TABLE tenants (
                         id                          UUID            NOT NULL DEFAULT gen_random_uuid(),
                         tenant_code                 VARCHAR(50)     NOT NULL,           -- e.g. BANK_001
                         schema_name                 VARCHAR(63)     NOT NULL,           -- PostgreSQL identifier limit 63 chars
                         institution_name            VARCHAR(255)    NOT NULL,
                         country_code                CHAR(2)         NOT NULL,           -- ISO 3166-1 alpha-2
                         regulatory_jurisdiction     VARCHAR(50)     NOT NULL,           -- FATF | FinCEN | FIU-IND
                         contact_email               VARCHAR(255)    NOT NULL,
                         contact_phone               VARCHAR(30),
                         address                     TEXT,
                         status                      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',

    -- Soft delete + who deleted
                         sys_is_deleted              BOOLEAN         NOT NULL DEFAULT FALSE,
                         sys_deleted_at              TIMESTAMPTZ,
                         sys_deleted_by              UUID,                               -- FK to platform_users (nullable)

    -- Audit
                         sys_created_by              UUID            NOT NULL,           -- FK to platform_users
                         sys_created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                         sys_updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                         CONSTRAINT pk_tenants               PRIMARY KEY (id),
                         CONSTRAINT uq_tenants_code          UNIQUE (tenant_code),
                         CONSTRAINT uq_tenants_schema        UNIQUE (schema_name),
                         CONSTRAINT chk_tenants_status       CHECK (status IN ('ACTIVE','SUSPENDED','DEPROVISIONED')),
                         CONSTRAINT chk_tenants_jurisdiction CHECK (regulatory_jurisdiction IN ('FATF','FinCEN','FIU-IND','MIXED')),
                         CONSTRAINT fk_tenants_created_by    FOREIGN KEY (sys_created_by) REFERENCES platform_users(id),
                         CONSTRAINT fk_tenants_deleted_by    FOREIGN KEY (sys_deleted_by) REFERENCES platform_users(id)
);

CREATE INDEX idx_tenants_status
    ON tenants (status)
    WHERE sys_is_deleted = FALSE;

CREATE INDEX idx_tenants_schema_name
    ON tenants (schema_name)
    WHERE sys_is_deleted = FALSE;

COMMENT ON TABLE  tenants               IS 'Each row is an isolated financial institution (tenant). schema_name is used as PostgreSQL search_path.';
COMMENT ON COLUMN tenants.schema_name   IS 'PostgreSQL schema name. Max 63 chars. Pattern: tenant_NNN_schema.';
COMMENT ON COLUMN tenants.status        IS 'ACTIVE=operational. SUSPENDED=sessions revoked. DEPROVISIONED=archived.';