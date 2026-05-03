INSERT INTO common_schema.global_rules (id, rule_name, rule_type, severity, base_risk_score)
VALUES ('01954425-a11b-74f7-8735-8664161f3647', 'Dormant Account Reactivation (High Value)', 'DORMANT_REACTIVATION', 'CRITICAL', 85);

INSERT INTO common_schema.global_rule_conditions (rule_id, condition_reference, attribute_name, threshold_value, value_data_type, aggregation_function, lookback_period)
VALUES ('01954425-a11b-74f7-8735-8664161f3647', 'GRC-DOR-001', 'Silence Period (Dormancy)', '0', 'STRING', 'MIN', '180 days');

INSERT INTO common_schema.global_rule_conditions (rule_id, condition_reference, attribute_name, threshold_value, value_data_type, aggregation_function, lookback_period)
VALUES ('01954425-a11b-74f7-8735-8664161f3647', 'GRC-DOR-002', 'Detection Window (Reactivation)', '0', 'STRING', 'MAX', '7 days');

INSERT INTO common_schema.global_rule_conditions (rule_id, condition_reference, attribute_name, threshold_value, value_data_type, aggregation_function)
VALUES ('01954425-a11b-74f7-8735-8664161f3647', 'GRC-DOR-003', 'Reactivation Amount Threshold', '5000', 'DECIMAL', 'NONE');