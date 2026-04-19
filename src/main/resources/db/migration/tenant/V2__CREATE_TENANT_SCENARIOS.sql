
-- ── 1. TENANT_SCENARIOS ──────────────────────────────────────
CREATE TABLE tenant_scenarios (
                                  id                  UUID            NOT NULL DEFAULT gen_random_uuid(),

    -- Cross-schema reference: common_schema.global_scenarios.id
    -- NOT a FK — enforced by TenantScenarioService at service layer
                                  global_scenario_id  UUID            NOT NULL,

                                  status              VARCHAR(10)     NOT NULL DEFAULT 'ACTIVE',

    -- Who activated this scenario (Bank Admin)
                                  sys_activated_by    UUID            NOT NULL,

    -- Audit timestamps
                                  sys_created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                  sys_updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- ── Constraints ──────────────────────────────────────────
                                  CONSTRAINT pk_tenant_scenarios
                                      PRIMARY KEY (id),

    -- Each global scenario can only be activated once per tenant
                                  CONSTRAINT uq_tenant_scenarios_global
                                      UNIQUE (global_scenario_id),

                                  CONSTRAINT chk_tenant_scenarios_status
                                      CHECK (status IN ('ACTIVE', 'PAUSED')),

                                  CONSTRAINT fk_tenant_scenarios_activated_by
                                      FOREIGN KEY (sys_activated_by)
                                          REFERENCES tenant_users (id)
);

-- ── Indexes ───────────────────────────────────────────────────
-- RuleEvaluator loads all ACTIVE scenarios per tenant at batch start
CREATE INDEX idx_tenant_scenarios_status
    ON tenant_scenarios (status);

-- ── Comments ──────────────────────────────────────────────────
COMMENT ON TABLE tenant_scenarios
    IS 'Activated global scenarios for this tenant. PAUSED scenarios are excluded from batch rule evaluation.';

COMMENT ON COLUMN tenant_scenarios.global_scenario_id
    IS 'Cross-schema ref to common_schema.global_scenarios.id. No FK constraint — enforced at service layer.';

COMMENT ON COLUMN tenant_scenarios.status
    IS 'ACTIVE = included in rule engine. PAUSED = excluded. Pausing does not affect already-generated alerts.';


-- ── 2. TENANT_SCENARIO_RULES ──────────────────────────────────
CREATE TABLE tenant_scenario_rules (
                                       id                  UUID            NOT NULL DEFAULT gen_random_uuid(),

    -- Parent scenario (within this tenant)
                                       tenant_scenario_id  UUID            NOT NULL,

    -- Cross-schema reference: common_schema.global_rules.id
    -- NOT a FK — enforced by TenantScenarioService at service layer
                                       global_rule_id      UUID            NOT NULL,

    -- Granular toggle: FALSE suppresses this specific rule
    -- within the scenario without pausing the whole scenario
                                       is_active           BOOLEAN         NOT NULL DEFAULT TRUE,

    -- Who last toggled this rule (Bank Admin or AML Admin)
                                       toggled_by          UUID            NOT NULL,

    -- Audit timestamps
                                       sys_created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                       sys_updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- ── Constraints ──────────────────────────────────────────
                                       CONSTRAINT pk_tenant_scenario_rules
                                           PRIMARY KEY (id),

    -- A rule can only appear once per tenant scenario
                                       CONSTRAINT uq_tenant_scenario_rule_pair
                                           UNIQUE (tenant_scenario_id, global_rule_id),

                                       CONSTRAINT fk_tsr_tenant_scenario
                                           FOREIGN KEY (tenant_scenario_id)
                                               REFERENCES tenant_scenarios (id),

                                       CONSTRAINT fk_tsr_toggled_by
                                           FOREIGN KEY (toggled_by)
                                               REFERENCES tenant_users (id)
);

-- ── Indexes ───────────────────────────────────────────────────
-- RuleEvaluator filters is_active=TRUE when loading rules for a scenario
CREATE INDEX idx_tsr_scenario_active
    ON tenant_scenario_rules (tenant_scenario_id, is_active);

-- Lookup by global_rule_id: "is this rule active in any scenario for this tenant?"
CREATE INDEX idx_tsr_global_rule
    ON tenant_scenario_rules (global_rule_id)
    WHERE is_active = TRUE;
