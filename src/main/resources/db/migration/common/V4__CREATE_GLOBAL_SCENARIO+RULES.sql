
SET search_path TO common_schema;

-- ── 1. GLOBAL_SCENARIOS ──────────────────────────────────────
CREATE TABLE global_scenarios (
                                  id              UUID            NOT NULL DEFAULT gen_random_uuid(),
                                  scenario_name   VARCHAR(100)    NOT NULL,
                                  category        VARCHAR(50)     NOT NULL,
                                  description     TEXT,
                                  created_by      UUID            NOT NULL,

    -- Soft delete
                                  sys_is_deleted  BOOLEAN         NOT NULL DEFAULT FALSE,
                                  sys_deleted_at  TIMESTAMPTZ,

    -- Audit
                                  sys_created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                  sys_updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                                  CONSTRAINT pk_global_scenarios          PRIMARY KEY (id),
                                  CONSTRAINT uq_global_scenarios_name     UNIQUE (scenario_name),
                                  CONSTRAINT chk_global_scenarios_cat     CHECK (category IN (
                                                                                              'Velocity','Geography','Round Amount','Structuring',
                                                                                              'PEP Exposure','Dormancy','Layering','Fraud Related')),
                                  CONSTRAINT fk_global_scenarios_creator  FOREIGN KEY (created_by) REFERENCES platform_users(id)
);

CREATE INDEX idx_global_scenarios_active
    ON global_scenarios (category)
    WHERE sys_is_deleted = FALSE;

-- ── 2. GLOBAL_RULES ──────────────────────────────────────────
CREATE TABLE global_rules (
                              id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
                              rule_name           VARCHAR(150)    NOT NULL,
    -- Stores boolean expression referencing condition_sequence numbers
    -- e.g. "1 AND (2 OR 3)" — parsed by RuleEvaluator at runtime
                              condition_logic     VARCHAR(255)    NOT NULL DEFAULT '1',
                              severity            VARCHAR(10)     NOT NULL DEFAULT 'MEDIUM',
                              base_risk_score     SMALLINT        NOT NULL DEFAULT 50
                                  CHECK (base_risk_score BETWEEN 0 AND 100),

    -- Soft delete
                              sys_is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
                              sys_deleted_at      TIMESTAMPTZ,

    -- Audit
                              sys_created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                              sys_updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                              CONSTRAINT pk_global_rules          PRIMARY KEY (id),
                              CONSTRAINT chk_global_rules_sev     CHECK (severity IN ('CRITICAL','HIGH','MEDIUM','LOW'))
);

CREATE INDEX idx_global_rules_active
    ON global_rules (severity)
    WHERE sys_is_deleted = FALSE;

-- ── 3. GLOBAL_SCENARIO_RULES (junction) ──────────────────────
CREATE TABLE global_scenario_rules (
                                       id              UUID        NOT NULL DEFAULT gen_random_uuid(),
                                       scenario_id     UUID        NOT NULL,
                                       rule_id         UUID        NOT NULL,
                                       priority_order  SMALLINT    NOT NULL DEFAULT 1,

                                       sys_created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                       CONSTRAINT pk_global_scenario_rules     PRIMARY KEY (id),
                                       CONSTRAINT uq_global_scenario_rule_pair UNIQUE (scenario_id, rule_id),
                                       CONSTRAINT fk_gsr_scenario  FOREIGN KEY (scenario_id) REFERENCES global_scenarios(id),
                                       CONSTRAINT fk_gsr_rule      FOREIGN KEY (rule_id)     REFERENCES global_rules(id)
);

CREATE INDEX idx_gsr_scenario ON global_scenario_rules (scenario_id);
CREATE INDEX idx_gsr_rule     ON global_scenario_rules (rule_id);

-- ── 4. GLOBAL_RULE_CONDITIONS ─────────────────────────────────
CREATE TABLE global_rule_conditions (
                                        id                          UUID            NOT NULL DEFAULT gen_random_uuid(),
                                        rule_id                     UUID            NOT NULL,
                                        attribute_id                UUID            NOT NULL,   -- FK to data_dictionary
                                        condition_sequence          SMALLINT        NOT NULL,  -- referenced in condition_logic

    -- Aggregation (velocity / sum checks)
                                        aggregation_function        VARCHAR(20)     NOT NULL DEFAULT 'NONE',
                                        lookback_period             VARCHAR(10),               -- e.g. '24h', '30d'. NULL = real-time

    -- Comparison
                                        operator                    VARCHAR(30)     NOT NULL,
                                        threshold_value             VARCHAR(255)    NOT NULL,
    -- Denormalized from data_dictionary.data_type for runtime perf
                                        value_data_type             VARCHAR(20)     NOT NULL DEFAULT 'NUMERIC',

                                        sys_created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                        sys_updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                                        CONSTRAINT pk_global_rule_conditions        PRIMARY KEY (id),
                                        CONSTRAINT uq_grc_rule_sequence             UNIQUE (rule_id, condition_sequence),
                                        CONSTRAINT fk_grc_rule      FOREIGN KEY (rule_id)      REFERENCES global_rules(id),
                                        CONSTRAINT fk_grc_attr      FOREIGN KEY (attribute_id) REFERENCES data_dictionary(id),
                                        CONSTRAINT chk_grc_agg      CHECK (aggregation_function IN ('NONE','SUM','COUNT','DISTINCT_COUNT')),
                                        CONSTRAINT chk_grc_op       CHECK (operator IN (
                                                                                        'GREATER_THAN','GREATER_THAN_OR_EQUAL',
                                                                                        'LESS_THAN','LESS_THAN_OR_EQUAL',
                                                                                        'EQUALS','NOT_EQUALS',
                                                                                        'IN','NOT_IN',
                                                                                        'CONTAINS','STARTS_WITH',
                                                                                        'IS_TRUE','IS_FALSE')),
                                        CONSTRAINT chk_grc_dtype    CHECK (value_data_type IN ('NUMERIC','STRING','BOOLEAN','DATE'))
);

CREATE INDEX idx_grc_rule ON global_rule_conditions (rule_id);

COMMENT ON COLUMN global_rule_conditions.condition_sequence  IS 'Used in condition_logic expression. Sequence 1,2,3 → "1 AND (2 OR 3)"';
COMMENT ON COLUMN global_rule_conditions.lookback_period     IS 'NULL = evaluate only the current transaction. Non-null = aggregate over past N hours/days.';
COMMENT ON COLUMN global_rule_conditions.value_data_type     IS 'Denormalized from data_dictionary. Prevents runtime join in RuleEvaluator hot path.';

CREATE TRIGGER trg_update_global_scenarios_timestamp BEFORE UPDATE ON global_scenarios FOR EACH ROW EXECUTE FUNCTION update_timestamp_column();
CREATE TRIGGER trg_update_global_rules_timestamp BEFORE UPDATE ON global_rules FOR EACH ROW EXECUTE FUNCTION update_timestamp_column();
CREATE TRIGGER trg_update_global_rule_conditions_timestamp BEFORE UPDATE ON global_rule_conditions FOR EACH ROW EXECUTE FUNCTION update_timestamp_column();