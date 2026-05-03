
CREATE TABLE cases (
                       id UUID PRIMARY KEY,
                       case_reference VARCHAR(50) UNIQUE NOT NULL, -- Format: CASE-YYYYMMDD-XXXXX

                       assigned_to UUID REFERENCES tenant_users(id) ON DELETE SET NULL, -- Current CO
                       assigned_by UUID REFERENCES tenant_users(id) ON DELETE SET NULL, -- Bank Admin


                       status VARCHAR(30) NOT NULL DEFAULT 'OPEN', -- OPEN / IN_PROGRESS / ESCALATED / CLOSED_STR / CLOSED_NO_ACTION
                       priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM', -- CRITICAL / HIGH / MEDIUM / LOW
                       aggregated_risk_score INT DEFAULT 0, -- Sum/Max of linked alert scores


                       opened_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       last_activity_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       closed_at TIMESTAMP,
                       closed_by UUID REFERENCES tenant_users(id) ON DELETE SET NULL,
                       closure_disposition VARCHAR(30), -- STR_FILED / FALSE_POSITIVE / INCONCLUSIVE
                       false_positive_rationale TEXT, -- Mandatory if disposition is FALSE_POSITIVE
                       has_investigation_note BOOLEAN NOT NULL DEFAULT FALSE, -- Gateway for STR filing

                       sys_is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                       sys_deleted_at TIMESTAMP,
                       sys_deleted_by UUID REFERENCES tenant_users(id) ON DELETE SET NULL,
                       sys_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       sys_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Attach the auto-update timestamp trigger (Function created in Tenant V1)
CREATE TRIGGER trg_cases_updated_at
    BEFORE UPDATE ON cases
    FOR EACH ROW
    EXECUTE FUNCTION update_sys_updated_at_column();

-- ------------------------------------------------------------------------------
-- 2. INVESTIGATION INDICES
-- ------------------------------------------------------------------------------

-- Critical for "My Cases" view (CO workspace)
CREATE INDEX idx_cases_assigned_to ON cases(assigned_to);

-- For the Admin Dashboard (Workload and priority management)
CREATE INDEX idx_cases_status_priority ON cases(status, priority);

-- For monitoring investigator responsiveness
CREATE INDEX idx_cases_last_activity ON cases(last_activity_at);

-- For filtering by closure results (Regulatory reporting)
CREATE INDEX idx_cases_closure ON cases(closure_disposition, closed_at);

-- For Search UI
CREATE INDEX idx_cases_reference ON cases(case_reference);

-- For Soft Deletion
CREATE INDEX idx_cases_sys_is_deleted ON cases(sys_is_deleted);