INSERT INTO common_schema.global_rules (id, rule_name, rule_type, severity, base_risk_score)
VALUES ('0195441b-4d7a-7589-9a2e-4b47604f8e6b', 'Suspicious Structuring (Under Threshold Smurfing)', 'STRUCTURING', 'HIGH', 85);

INSERT INTO common_schema.global_rule_conditions (rule_id, condition_reference, attribute_name, threshold_value, value_data_type, aggregation_function, lookback_period)
VALUES ('0195441b-4d7a-7589-9a2e-4b47604f8e6b', 'GRC-STR-001', 'Individual Transaction Ceiling', '10000', 'DECIMAL', 'NONE', '7 days');

INSERT INTO common_schema.global_rule_conditions (rule_id, condition_reference, attribute_name, threshold_value, value_data_type, aggregation_function)
VALUES ('0195441b-4d7a-7589-9a2e-4b47604f8e6b', 'GRC-STR-002', 'Total Cumulative Amount', '50000', 'DECIMAL', 'SUM');

INSERT INTO common_schema.global_rule_conditions (rule_id, condition_reference, attribute_name, threshold_value, value_data_type, aggregation_function)
VALUES ('0195441b-4d7a-7589-9a2e-4b47604f8e6b', 'GRC-STR-003', 'Minimum Split Count', '5', 'INTEGER', 'COUNT');