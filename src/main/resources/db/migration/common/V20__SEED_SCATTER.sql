INSERT INTO common_schema.global_rules (id, rule_name, rule_type, severity, base_risk_score)
VALUES ('019c00d4-1a9e-7f32-b06d-8e4a2c5b9f31', 'Scatter Disbursement (Sent to 5+ distinct accounts in 24h)', 'SCATTER', 'HIGH', 85);

INSERT INTO common_schema.global_rule_conditions (rule_id, condition_reference, attribute_name, threshold_value, value_data_type, aggregation_function, lookback_period)
VALUES ('019c00d4-1a9e-7f32-b06d-8e4a2c5b9f31', 'GRC-SCT-001', 'distinct_beneficiary_count', '5', 'INTEGER', 'COUNT', '24h');