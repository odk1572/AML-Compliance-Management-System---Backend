
-- ── 1. CASES ─────────────────────────────────────────────────
CREATE TABLE cases (
                       id                          UUID            NOT NULL DEFAULT gen_random_uuid(),

    -- Human-readable reference: CASE-YYYYMMDD-XXXXX
                       case_reference              VARCHAR(25)     NOT NULL,

    -- Current assigned CO (NULL when case is in reassignment queue)
                       assigned_to                 UUID,

    -- Bank Admin who created this case
                       assigned_by                 UUID            NOT NULL,

                       status                      VARCHAR(20)     NOT NULL DEFAULT 'OPEN',
                       priority                    VARCHAR(10)     NOT NULL DEFAULT 'MEDIUM',

    -- Denormalized: sum of risk_score across all linked alert rows.
    -- Recalculated by CaseAssignmentService.createCase() and
    -- whenever a new alert is linked via CASE_ALERT_LINKS.
                       aggregated_risk_score       SMALLINT        NOT NULL DEFAULT 0
                           CHECK (aggregated_risk_score >= 0),

    -- Lifecycle timestamps
                       opened_at                   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                       last_activity_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                       closed_at                   TIMESTAMPTZ,
                       closed_by                   UUID,

    -- Closure details
                       closure_disposition         VARCHAR(15),
    -- Mandatory when closure_disposition = 'FALSE_POSITIVE'
    -- DB CHECK constraint enforces minimum length — cannot be bypassed
                       false_positive_rationale    TEXT,

    -- STR filing gate — set TRUE by trigger on first CASE_NOTES insert.
    -- StrFilingService.validateGate() checks this before allowing filing.
                       has_investigation_note      BOOLEAN         NOT NULL DEFAULT FALSE,

    -- Soft delete (admin archival only — closed cases always retained)
                       sys_is_deleted              BOOLEAN         NOT NULL DEFAULT FALSE,
                       sys_deleted_at              TIMESTAMPTZ,
                       sys_deleted_by              UUID,

    -- Audit timestamps
                       sys_created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                       sys_updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- ── Constraints ──────────────────────────────────────────
                       CONSTRAINT pk_cases
                           PRIMARY KEY (id),

                       CONSTRAINT uq_cases_reference
                           UNIQUE (case_reference),

                       CONSTRAINT chk_cases_status
                           CHECK (status IN (
                                             'OPEN',
                                             'IN_PROGRESS',
                                             'ESCALATED',
                                             'CLOSED_STR',
                                             'CLOSED_NO_ACTION'
                               )),

                       CONSTRAINT chk_cases_priority
                           CHECK (priority IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),

                       CONSTRAINT chk_cases_closure_disposition
                           CHECK (
                               closure_disposition IS NULL OR
                               closure_disposition IN ('STR_FILED', 'FALSE_POSITIVE', 'INCONCLUSIVE')
                               ),

    -- DB-level enforcement: false positive MUST have a rationale
    -- length > 10 to prevent single-word dismissals
                       CONSTRAINT chk_cases_fp_rationale
                           CHECK (
                               closure_disposition IS DISTINCT FROM 'FALSE_POSITIVE' OR
                               (
                               false_positive_rationale IS NOT NULL AND
                               length(trim(false_positive_rationale)) > 10
                               )
),

    -- closed_at must be set when case reaches a terminal status
    CONSTRAINT chk_cases_closed_at
        CHECK (
            status NOT IN ('CLOSED_STR', 'CLOSED_NO_ACTION') OR
            (closed_at IS NOT NULL AND closed_by IS NOT NULL)
        ),

    CONSTRAINT fk_cases_assigned_to
        FOREIGN KEY (assigned_to)
        REFERENCES tenant_users (id),

    CONSTRAINT fk_cases_assigned_by
        FOREIGN KEY (assigned_by)
        REFERENCES tenant_users (id),

    CONSTRAINT fk_cases_closed_by
        FOREIGN KEY (closed_by)
        REFERENCES tenant_users (id),

    CONSTRAINT fk_cases_deleted_by
        FOREIGN KEY (sys_deleted_by)
        REFERENCES tenant_users (id)
);

-- ── Indexes ───────────────────────────────────────────────────
-- CO Investigation Dashboard — cases assigned to me, filtered by status
CREATE INDEX idx_cases_assigned_status
    ON cases (assigned_to, status, priority)
    WHERE sys_is_deleted = FALSE;

-- Bank Admin case tracking board — all open cases ordered by priority + time
CREATE INDEX idx_cases_status_priority_time
    ON cases (status, priority, opened_at DESC)
    WHERE sys_is_deleted = FALSE;

-- CO workload report — count open/in-progress per CO
CREATE INDEX idx_cases_assigned_open
    ON cases (assigned_to)
    WHERE status IN ('OPEN', 'IN_PROGRESS') AND sys_is_deleted = FALSE;

-- Escalated case surfacing on Bank Admin dashboard
CREATE INDEX idx_cases_escalated
    ON cases (status, last_activity_at DESC)
    WHERE status = 'ESCALATED' AND sys_is_deleted = FALSE;

-- ── TRIGGER: set has_investigation_note = TRUE on first note ──
-- Fires AFTER INSERT on case_notes (defined after that table).
-- Declared here as a function; trigger body added after V10.

CREATE OR REPLACE FUNCTION fn_set_has_investigation_note()
RETURNS TRIGGER AS $$
BEGIN
UPDATE cases
SET    has_investigation_note = TRUE,
       last_activity_at       = NOW(),
       sys_updated_at         = NOW()
WHERE  id = NEW.case_id
  AND    has_investigation_note = FALSE;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- ── 2. CASE_ALERT_LINKS ───────────────────────────────────────
CREATE TABLE case_alert_links (
                                  id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
                                  case_id             UUID        NOT NULL,
                                  alert_id            UUID        NOT NULL,
                                  linked_by           UUID        NOT NULL,
    -- TRUE on the alert that initiated case creation
                                  is_primary_alert    BOOLEAN     NOT NULL DEFAULT FALSE,

    -- Immutable link record
                                  sys_created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- ── Constraints ──────────────────────────────────────────
                                  CONSTRAINT pk_case_alert_links
                                      PRIMARY KEY (id),

    -- One alert cannot be linked to the same case twice
                                  CONSTRAINT uq_case_alert_pair
                                      UNIQUE (case_id, alert_id),

                                  CONSTRAINT fk_cal_case
                                      FOREIGN KEY (case_id)
                                          REFERENCES cases (id),

                                  CONSTRAINT fk_cal_alert
                                      FOREIGN KEY (alert_id)
                                          REFERENCES alerts (id),

                                  CONSTRAINT fk_cal_linked_by
                                      FOREIGN KEY (linked_by)
                                          REFERENCES tenant_users (id)
);

-- Case detail view: all alerts for a case
CREATE INDEX idx_cal_case     ON case_alert_links (case_id);
-- Alert detail view: which case(s) contain this alert
CREATE INDEX idx_cal_alert    ON case_alert_links (alert_id);


-- ── 3. CASE_NOTES ─────────────────────────────────────────────
CREATE TABLE case_notes (
                            id                      UUID            NOT NULL DEFAULT gen_random_uuid(),
                            case_id                 UUID            NOT NULL,
                            authored_by             UUID            NOT NULL,
                            note_type               VARCHAR(20)     NOT NULL DEFAULT 'OBSERVATION',
                            note_content            TEXT            NOT NULL,

    -- Optional evidence attachment via Cloudinary
                            cloudinary_public_id    VARCHAR(255),
                            cloudinary_secure_url   TEXT,

    -- Immutable after insert — legal/regulatory requirement
    -- No sys_updated_at. No sys_is_deleted.
                            sys_created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- ── Constraints ──────────────────────────────────────────
                            CONSTRAINT pk_case_notes
                                PRIMARY KEY (id),

                            CONSTRAINT chk_cn_note_type
                                CHECK (note_type IN ('OBSERVATION', 'EVIDENCE', 'RATIONALE', 'SYSTEM')),

                            CONSTRAINT chk_cn_content_not_empty
                                CHECK (length(trim(note_content)) > 0),

                            CONSTRAINT fk_cn_case
                                FOREIGN KEY (case_id)
                                    REFERENCES cases (id),

                            CONSTRAINT fk_cn_authored_by
                                FOREIGN KEY (authored_by)
                                    REFERENCES tenant_users (id)
);

CREATE INDEX idx_case_notes_case
    ON case_notes (case_id, sys_created_at ASC);

-- Attach trigger: sets has_investigation_note=TRUE on first insert
CREATE TRIGGER trg_case_note_set_investigation_flag
    AFTER INSERT ON case_notes
    FOR EACH ROW
    EXECUTE FUNCTION fn_set_has_investigation_note();

-- Immutability rules
CREATE OR REPLACE RULE no_update_case_notes AS
    ON UPDATE TO case_notes DO INSTEAD NOTHING;

CREATE OR REPLACE RULE no_delete_case_notes AS
    ON DELETE TO case_notes DO INSTEAD NOTHING;


-- ── 4. CASE_ASSIGNMENTS ───────────────────────────────────────
CREATE TABLE case_assignments (
                                  id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
                                  case_id             UUID            NOT NULL,
    -- NULL for the first assignment (no previous CO existed)
                                  assigned_from       UUID,
                                  assigned_to         UUID            NOT NULL,
                                  assigned_by         UUID            NOT NULL,
                                  assignment_reason   VARCHAR(500),

    -- Immutable history record
                                  sys_created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- ── Constraints ──────────────────────────────────────────
                                  CONSTRAINT pk_case_assignments
                                      PRIMARY KEY (id),

                                  CONSTRAINT fk_ca_case
                                      FOREIGN KEY (case_id)
                                          REFERENCES cases (id),

                                  CONSTRAINT fk_ca_assigned_from
                                      FOREIGN KEY (assigned_from)
                                          REFERENCES tenant_users (id),

                                  CONSTRAINT fk_ca_assigned_to
                                      FOREIGN KEY (assigned_to)
                                          REFERENCES tenant_users (id),

                                  CONSTRAINT fk_ca_assigned_by
                                      FOREIGN KEY (assigned_by)
                                          REFERENCES tenant_users (id)
);

CREATE INDEX idx_case_assignments_case
    ON case_assignments (case_id, sys_created_at DESC);

-- CO performance report: all assignments TO a specific CO
CREATE INDEX idx_case_assignments_to
    ON case_assignments (assigned_to, sys_created_at DESC);


-- ── 5. CASE_ESCALATIONS ───────────────────────────────────────
CREATE TABLE case_escalations (
                                  id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
                                  case_id             UUID            NOT NULL,
                                  escalated_by        UUID            NOT NULL,
                                  escalated_to        UUID            NOT NULL,
                                  escalation_reason   TEXT            NOT NULL,
                                  escalation_status   VARCHAR(15)     NOT NULL DEFAULT 'PENDING',
                                  acknowledged_at     TIMESTAMPTZ,
                                  resolved_at         TIMESTAMPTZ,

    -- Stateful — escalation_status transitions over lifecycle
                                  sys_created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                  sys_updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- ── Constraints ──────────────────────────────────────────
                                  CONSTRAINT pk_case_escalations
                                      PRIMARY KEY (id),

                                  CONSTRAINT chk_ce_status
                                      CHECK (escalation_status IN ('PENDING', 'ACKNOWLEDGED', 'RESOLVED')),

                                  CONSTRAINT chk_ce_reason_not_empty
                                      CHECK (length(trim(escalation_reason)) > 0),

    -- acknowledged_at must be set when status = ACKNOWLEDGED or RESOLVED
                                  CONSTRAINT chk_ce_acknowledged_at
                                      CHECK (
                                          escalation_status = 'PENDING' OR
                                          acknowledged_at IS NOT NULL
                                          ),

    -- resolved_at must be set when status = RESOLVED
                                  CONSTRAINT chk_ce_resolved_at
                                      CHECK (
                                          escalation_status != 'RESOLVED' OR
                                          resolved_at IS NOT NULL
),

    CONSTRAINT fk_ce_case
        FOREIGN KEY (case_id)
        REFERENCES cases (id),

    CONSTRAINT fk_ce_escalated_by
        FOREIGN KEY (escalated_by)
        REFERENCES tenant_users (id),

    CONSTRAINT fk_ce_escalated_to
        FOREIGN KEY (escalated_to)
        REFERENCES tenant_users (id)
);

CREATE INDEX idx_ce_case    ON case_escalations (case_id, sys_created_at DESC);
CREATE INDEX idx_ce_pending ON case_escalations (escalated_to, escalation_status)
    WHERE escalation_status = 'PENDING';
