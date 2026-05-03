CREATE TABLE alerts (
                        id UUID PRIMARY KEY,

                        customer_profile_id UUID NOT NULL REFERENCES customer_profiles(id) ON DELETE RESTRICT,

                        transaction_id UUID NOT NULL REFERENCES transactions(id) ON DELETE RESTRICT,

                        tenant_scenario_id UUID NOT NULL REFERENCES tenant_scenarios(id) ON DELETE RESTRICT,

                        global_scenario_id UUID NOT NULL REFERENCES common_schema.global_scenarios(id) ON DELETE RESTRICT,
                        global_rule_id UUID NOT NULL REFERENCES common_schema.global_rules(id) ON DELETE RESTRICT,


                        tenant_rule_id UUID REFERENCES tenant_rules(id) ON DELETE SET NULL,

                        alert_reference VARCHAR(50) UNIQUE NOT NULL, -- Format: ALT-YYYYMMDD-XXXXX
                        severity VARCHAR(20) NOT NULL, -- HIGH / MEDIUM / LOW
                        status VARCHAR(50) NOT NULL DEFAULT 'NEW', -- NEW / DISMISSED / BUNDLED_TO_CASE / CLOSED_CONFIRMED / CLOSED_FALSE_POSITIVE
                        typology_triggered VARCHAR(255) NOT NULL, -- e.g., 'Structuring'
                        risk_score DECIMAL(5, 2) NOT NULL DEFAULT 0.00, -- Computed 0-100

                        sys_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        sys_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_alerts_updated_at
    BEFORE UPDATE ON alerts
    FOR EACH ROW
    EXECUTE FUNCTION update_sys_updated_at_column();


CREATE INDEX idx_alerts_customer_profile_id ON alerts(customer_profile_id);
CREATE INDEX idx_alerts_transaction_id ON alerts(transaction_id);
CREATE INDEX idx_alerts_tenant_scenario_id ON alerts(tenant_scenario_id);
CREATE INDEX idx_alerts_status_severity ON alerts(status, severity);
CREATE INDEX idx_alerts_typology ON alerts(typology_triggered);
CREATE INDEX idx_alerts_tenant_rule_id ON alerts(tenant_rule_id);
CREATE INDEX idx_alerts_created_at ON alerts(sys_created_at);
CREATE INDEX idx_alerts_reference ON alerts(alert_reference);