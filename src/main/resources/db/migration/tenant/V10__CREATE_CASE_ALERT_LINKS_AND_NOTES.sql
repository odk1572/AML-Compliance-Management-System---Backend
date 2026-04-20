CREATE TABLE case_alert_links (
                                  id UUID PRIMARY KEY,
                                  case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
                                  alert_id UUID NOT NULL REFERENCES alerts(id) ON DELETE CASCADE,
                                  linked_by UUID NOT NULL REFERENCES tenant_users(id),
                                  is_primary_alert BOOLEAN NOT NULL DEFAULT FALSE,

    -- Immutable record
                                  sys_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Prevents the same alert from being linked to the same case twice
                                  CONSTRAINT uk_case_alert_link UNIQUE (case_id, alert_id)
);

-- Indices for rapid case assembly
CREATE INDEX idx_cal_case_id ON case_alert_links(case_id);
CREATE INDEX idx_cal_alert_id ON case_alert_links(alert_id);

CREATE TABLE case_notes (
                            id UUID PRIMARY KEY,
                            case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
                            authored_by UUID NOT NULL REFERENCES tenant_users(id),
                            note_type VARCHAR(50) NOT NULL, -- OBSERVATION / EVIDENCE / RATIONALE
                            note_content TEXT NOT NULL,

    -- Immutable record
                            sys_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indices for fetching case history
CREATE INDEX idx_case_notes_case_id ON case_notes(case_id);
CREATE INDEX idx_case_notes_authored_by ON case_notes(authored_by);
CREATE INDEX idx_case_notes_created_at ON case_notes(sys_created_at);