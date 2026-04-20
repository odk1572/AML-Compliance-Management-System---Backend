CREATE TABLE case_assignments (
                                  id UUID PRIMARY KEY,
                                  case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,

    -- Handover Details
                                  assigned_from UUID REFERENCES tenant_users(id) ON DELETE SET NULL, -- Previous CO
                                  assigned_to UUID NOT NULL REFERENCES tenant_users(id) ON DELETE SET NULL, -- New CO
                                  assigned_by UUID NOT NULL REFERENCES tenant_users(id) ON DELETE SET NULL, -- Admin/System

                                  assignment_reason TEXT,

    -- Immutable record of the handover event
                                  sys_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ca_case_id ON case_assignments(case_id);
CREATE INDEX idx_ca_assigned_to ON case_assignments(assigned_to);
CREATE INDEX idx_ca_created_at ON case_assignments(sys_created_at);

CREATE TABLE case_escalations (
                                  id UUID PRIMARY KEY,
                                  case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,

    -- Hierarchy Details
                                  escalated_by UUID NOT NULL REFERENCES tenant_users(id) ON DELETE SET NULL,
                                  escalated_to UUID NOT NULL REFERENCES tenant_users(id) ON DELETE SET NULL,

                                  escalation_reason TEXT NOT NULL,
                                  escalation_status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING / ACKNOWLEDGED / RESOLVED

    -- Lifecycle Timestamps
                                  acknowledged_at TIMESTAMP,
                                  resolved_at TIMESTAMP,

                                  sys_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  sys_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_case_escalations_updated_at
    BEFORE UPDATE ON case_escalations
    FOR EACH ROW
    EXECUTE FUNCTION update_sys_updated_at_column();

CREATE INDEX idx_ce_case_id ON case_escalations(case_id);
CREATE INDEX idx_ce_escalated_to ON case_escalations(escalated_to);
CREATE INDEX idx_ce_status ON case_escalations(escalation_status);
CREATE INDEX idx_ce_created_at ON case_escalations(sys_created_at);