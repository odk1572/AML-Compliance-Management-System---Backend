
-- ── 1. TENANT_RULES ──────────────────────────────────────────
CREATE TABLE tenant_rules (
                              id              UUID            NOT NULL DEFAULT gen_random_uuid(),

    -- Cross-schema reference: common_schema.global_rules.id
    -- NOT a FK — enforced at service layer
                              global_rule_id  UUID            NOT NULL,

    -- Bank's internal reference code (e.g. BA-STR-001)
                              rule_code       VARCHAR(30)     NOT NULL,

    -- Bank's custom display name for the rule
                              rule_name       VARCHAR(150)    NOT NULL,

    -- Who created this tenant rule binding (Bank Admin)
                              sys_created_by  UUID            NOT NULL,

    -- Soft delete
                              sys_is_deleted  BOOLEAN         NOT NULL DEFAULT FALSE,
                              sys_deleted_at  TIMESTAMPTZ,

    -- Audit timestamps
                              sys_created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                              sys_updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- ── Constraints ──────────────────────────────────────────
                              CONSTRAINT pk_tenant_rules
                                  PRIMARY KEY (id),

                              CONSTRAINT uq_tenant_rules_code
                                  UNIQUE (rule_code),

                              CONSTRAINT chk_tenant_rules_code_format
                                  CHECK (rule_code ~ '^[A-Z0-9\-]+$'),

    CONSTRAINT fk_tenant_rules_created_by
        FOREIGN KEY (sys_created_by)
        REFERENCES tenant_users (id)
);

-- ── Indexes ───────────────────────────────────────────────────
-- Used by TenantRuleVersioningService to find a rule by its global counterpart
CREATE INDEX idx_tenant_rules_global
    ON tenant_rules (global_rule_id)
    WHERE sys_is_deleted = FALSE;


-- ── 2. TENANT_RULE_VERSIONS ───────────────────────────────────
CREATE TABLE tenant_rule_versions (
                                      id                  UUID            NOT NULL DEFAULT gen_random_uuid(),

    -- Parent rule (within this tenant schema)
                                      rule_id             UUID            NOT NULL,

    -- Monotonically increasing per rule_id
                                      version_number      SMALLINT        NOT NULL DEFAULT 1,

    -- What changed in this version
                                      change_type         VARCHAR(25)     NOT NULL,

    -- Lifecycle status of this version
                                      status              VARCHAR(10)     NOT NULL DEFAULT 'ACTIVE',

    -- Override values — NULL means inherit from global_rules
                                      severity            VARCHAR(10),
                                      base_risk_score     SMALLINT        CHECK (base_risk_score BETWEEN 0 AND 100),

    -- Mandatory justification for every version creation
                                      change_reason       VARCHAR(500)    NOT NULL,

    -- Version pointer flags
                                      is_current_version  BOOLEAN         NOT NULL DEFAULT TRUE,
                                      effective_from      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    -- Set to NOW() when superseded by a newer version; NULL = still current
                                      effective_to        TIMESTAMPTZ,

    -- Immutable snapshot — sys_created_at only, no sys_updated_at
    -- (archiving a version writes effective_to — the only allowed update)
                                      sys_changed_by      UUID            NOT NULL,
                                      sys_created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- ── Constraints ──────────────────────────────────────────
                                      CONSTRAINT pk_tenant_rule_versions
                                          PRIMARY KEY (id),

    -- Version number must be unique per rule
                                      CONSTRAINT uq_trv_rule_version
                                          UNIQUE (rule_id, version_number),

                                      CONSTRAINT chk_trv_change_type
                                          CHECK (change_type IN (
                                                                 'INITIAL',
                                                                 'THRESHOLD_UPDATE',
                                                                 'SEVERITY_CHANGE',
                                                                 'VELOCITY_CHANGE',
                                                                 'STATUS_CHANGE'
                                              )),

                                      CONSTRAINT chk_trv_status
                                          CHECK (status IN ('ACTIVE', 'PAUSED', 'ARCHIVED')),

                                      CONSTRAINT chk_trv_severity
                                          CHECK (severity IS NULL OR severity IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),

                                      CONSTRAINT chk_trv_change_reason
                                          CHECK (length(change_reason) >= 5),

    -- effective_to must be after effective_from when set
                                      CONSTRAINT chk_trv_effective_range
                                          CHECK (effective_to IS NULL OR effective_to > effective_from),

    -- Only one version can be current per rule at a time
    -- Partial unique index enforces this below (CHECK constraints
    -- cannot reference other rows, so index is the right tool)

                                      CONSTRAINT fk_trv_rule
                                          FOREIGN KEY (rule_id)
                                              REFERENCES tenant_rules (id),

                                      CONSTRAINT fk_trv_changed_by
                                          FOREIGN KEY (sys_changed_by)
                                              REFERENCES tenant_users (id)
);

-- ── Indexes ───────────────────────────────────────────────────
-- RuleEvaluator loads the current version per rule — HOT PATH
-- Partial unique index: only one CURRENT version allowed per rule
CREATE UNIQUE INDEX idx_trv_one_current_per_rule
    ON tenant_rule_versions (rule_id)
    WHERE is_current_version = TRUE;

-- Version history view (ordered by version_number DESC)
CREATE INDEX idx_trv_rule_history
    ON tenant_rule_versions (rule_id, version_number DESC);


-- ── 3. TENANT_RULE_THRESHOLDS ─────────────────────────────────
CREATE TABLE tenant_rule_thresholds (
                                        id                              UUID            NOT NULL DEFAULT gen_random_uuid(),

    -- Parent version snapshot
                                        rule_version_id                 UUID            NOT NULL,

    -- Cross-schema reference: common_schema.global_rule_conditions.id
    -- NOT a FK — enforced at service layer
                                        global_condition_id             UUID            NOT NULL,

    -- Per-condition override values.
    -- NULL = fall back to global_rule_conditions value for that field.
                                        override_value                  VARCHAR(255),
                                        override_lookback_period        VARCHAR(10),        -- e.g. '7d', '48h', '30d'
                                        override_aggregation_function   VARCHAR(20),        -- e.g. 'COUNT', 'SUM', 'DISTINCT_COUNT'

    -- No independent audit fields — fully owned by the parent
    -- version's immutable snapshot. sys_created_at inherited via
    -- version creation transaction.

    -- ── Constraints ──────────────────────────────────────────
                                        CONSTRAINT pk_tenant_rule_thresholds
                                            PRIMARY KEY (id),

    -- One override row per condition per version
                                        CONSTRAINT uq_trt_version_condition
                                            UNIQUE (rule_version_id, global_condition_id),

                                        CONSTRAINT chk_trt_override_agg
                                            CHECK (
                                                override_aggregation_function IS NULL OR
                                                override_aggregation_function IN ('NONE', 'SUM', 'COUNT', 'DISTINCT_COUNT')
                                                ),

                                        CONSTRAINT chk_trt_lookback_format
                                            CHECK (
                                                override_lookback_period IS NULL OR
                                                override_lookback_period ~ '^\d+[hdwm]$'
),

    CONSTRAINT fk_trt_version
        FOREIGN KEY (rule_version_id)
        REFERENCES tenant_rule_versions (id)
);

-- ── Indexes ───────────────────────────────────────────────────
-- RuleEvaluator loads all thresholds for a version in one query
CREATE INDEX idx_trt_version
    ON tenant_rule_thresholds (rule_version_id);
