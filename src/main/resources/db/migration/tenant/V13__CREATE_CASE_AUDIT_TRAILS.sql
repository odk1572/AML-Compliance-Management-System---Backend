CREATE TABLE case_audit_trail (
                                  id UUID PRIMARY KEY,
                                  case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
                                  actor_id UUID REFERENCES tenant_users(id) ON DELETE SET NULL,

                                  event_type VARCHAR(50) NOT NULL,
    -- CREATED / ASSIGNED / REASSIGNED / NOTE_ADDED / ESCALATED / STR_FILED / CLOSED

                                  event_metadata JSONB, -- Stores context (e.g., {"old_owner": "UUID", "new_owner": "UUID"})
                                  ip_address VARCHAR(45),

    -- Immutable record
                                  sys_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraint to ensure only valid lifecycle events are recorded
                                  CONSTRAINT chk_case_event_type CHECK (event_type IN (
                                                                                       'CREATED',
                                                                                       'ASSIGNED',
                                                                                       'REASSIGNED',
                                                                                       'NOTE_ADDED',
                                                                                       'ESCALATED',
                                                                                       'STR_FILED',
                                                                                       'CLOSED'
                                      ))
);


-- Critical for the Case Timeline UI:
-- "Show me everything that happened to Case X in order"
CREATE INDEX idx_cat_case_id_time ON case_audit_trail(case_id, sys_created_at ASC);

-- For tracking a specific investigator's footprint across all cases
CREATE INDEX idx_cat_actor_id ON case_audit_trail(actor_id);

-- For filtering by high-impact events (e.g., "Show me all STR_FILED events this month")
CREATE INDEX idx_cat_event_type ON case_audit_trail(event_type);