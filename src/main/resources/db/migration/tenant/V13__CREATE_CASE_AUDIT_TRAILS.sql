CREATE TABLE case_audit_trail (
                                  id UUID PRIMARY KEY,
                                  case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
                                  actor_id UUID REFERENCES tenant_users(id) ON DELETE SET NULL,

                                  event_type VARCHAR(50) NOT NULL,

                                  event_metadata JSONB, -- Stores context (e.g., {"old_owner": "UUID", "new_owner": "UUID"})
                                  ip_address VARCHAR(45),

                                  sys_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

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


CREATE INDEX idx_cat_case_id_time ON case_audit_trail(case_id, sys_created_at ASC);

CREATE INDEX idx_cat_actor_id ON case_audit_trail(actor_id);

CREATE INDEX idx_cat_event_type ON case_audit_trail(event_type);