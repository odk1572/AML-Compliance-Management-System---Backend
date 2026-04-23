CREATE SCHEMA IF NOT EXISTS common_schema;
SET search_path TO common_schema;


CREATE TABLE platform_audit_log (
                                    id UUID PRIMARY KEY,
                                    actor_id UUID REFERENCES platform_users(id) ON DELETE SET NULL,
                                    actor_role VARCHAR(50) NOT NULL,
                                    tenant_id UUID REFERENCES tenants(id) ON DELETE SET NULL,
                                    schema_name VARCHAR(63),
                                    action_category VARCHAR(50) NOT NULL,
                                    action_performed VARCHAR(255) NOT NULL,
                                    target_entity_type VARCHAR(100) NOT NULL,
                                    target_entity_id UUID,
                                    previous_state JSONB,
                                    new_state JSONB,
                                    ip_address VARCHAR(45),
                                    user_agent TEXT,
                                    sys_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_platform_audit_actor_id ON platform_audit_log(actor_id);
CREATE INDEX idx_platform_audit_tenant_id ON platform_audit_log(tenant_id);
CREATE INDEX idx_platform_audit_category ON platform_audit_log(action_category);
CREATE INDEX idx_platform_audit_target_entity ON platform_audit_log(target_entity_type, target_entity_id);
CREATE INDEX idx_platform_audit_created_at ON platform_audit_log(sys_created_at);

ALTER TABLE global_rule_conditions ADD CONSTRAINT chk_grc_attribute_name
    CHECK (attribute_name IN (
                              'amount',
                              'currency_code',
                              'originator_country',
                              'beneficiary_country',
                              'transaction_type',
                              'channel',
                              'transaction_timestamp',
                              'risk_rating',
                              'is_pep',
                              'is_dormant',
                              'account_age_days',
                              'kyc_status',
                              'monthly_income',
                              'net_worth'
        ));