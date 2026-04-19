
CREATE TABLE case_audit_trail (
                                  id              UUID            NOT NULL DEFAULT gen_random_uuid(),

    -- Case this event belongs to
                                  case_id         UUID            NOT NULL,

    -- Actor who triggered this event (CO, Bank Admin, or system)
                                  actor_id        UUID            NOT NULL,

    -- Event classification
                                  event_type      VARCHAR(20)     NOT NULL,

    -- Rich event-specific context (see header for schema per type)
                                  event_metadata  JSONB           NOT NULL DEFAULT '{}'::JSONB,

    -- Request metadata for security audit
                                  ip_address      VARCHAR(45),

    -- Single immutable timestamp
                                  sys_created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- ── Constraints ──────────────────────────────────────────
                                  CONSTRAINT pk_case_audit_trail
                                      PRIMARY KEY (id),

                                  CONSTRAINT chk_cat_event_type
                                      CHECK (event_type IN (
                                                            'CREATED',          -- case first created and assigned
                                                            'ASSIGNED',         -- initial assignment to CO
                                                            'REASSIGNED',       -- case moved to different CO
                                                            'STATUS_CHANGED',   -- any status transition
                                                            'NOTE_ADDED',       -- CO added an investigation note
                                                            'ESCALATED',        -- CO escalated to Bank Admin
                                                            'STR_FILED',        -- STR/SAR filing submitted
                                                            'CLOSED',           -- case closed (either disposition)
                                                            'VIEWED'            -- CO or admin viewed the case (optional)
                                          )),

                                  CONSTRAINT fk_cat_case
                                      FOREIGN KEY (case_id)
                                          REFERENCES cases (id),

                                  CONSTRAINT fk_cat_actor
                                      FOREIGN KEY (actor_id)
                                          REFERENCES tenant_users (id)
);

-- ── Indexes ───────────────────────────────────────────────────
-- Primary access pattern: chronological events for one case
-- Used by CaseInvestigationService.getCaseAuditTrail()
-- and CaseInvestigationService.exportAuditTrailAsPdf()
CREATE INDEX idx_cat_case_time
    ON case_audit_trail (case_id, sys_created_at ASC);

-- Actor-level audit: all case events by a specific CO
CREATE INDEX idx_cat_actor_time
    ON case_audit_trail (actor_id, sys_created_at DESC);

-- Event type filtering (reporting + compliance officer performance)
CREATE INDEX idx_cat_event_type
    ON case_audit_trail (event_type, sys_created_at DESC);

-- ── IMMUTABILITY ENFORCEMENT ──────────────────────────────────
CREATE OR REPLACE RULE no_update_case_audit_trail AS
    ON UPDATE TO case_audit_trail
                  DO INSTEAD NOTHING;

CREATE OR REPLACE RULE no_delete_case_audit_trail AS
    ON DELETE TO case_audit_trail
    DO INSTEAD NOTHING;
