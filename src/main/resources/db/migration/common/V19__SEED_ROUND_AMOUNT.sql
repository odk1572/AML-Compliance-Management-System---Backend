INSERT INTO common_schema.global_rules (id, rule_name, rule_type, severity, base_risk_score)
VALUES ('019c00c8-471a-7b3e-8f5d-2c9e4b6a8f11', 'Frequent Round Amount Transfers (Multiples of amounts like 1k 2k)', 'ROUND_AMOUNT', 'HIGH', 80);

INSERT INTO common_schema.global_rule_conditions (rule_id, condition_reference, attribute_name, threshold_value, value_data_type, aggregation_function, lookback_period)
VALUES ('019c00c8-471a-7b3e-8f5d-2c9e4b6a8f11', 'GRC-RND-001', 'amount_divisor', '1000', 'INTEGER', 'NONE', '7d');

INSERT INTO common_schema.global_rule_conditions (rule_id, condition_reference, attribute_name, threshold_value, value_data_type, aggregation_function, lookback_period)
VALUES ('019c00c8-471a-7b3e-8f5d-2c9e4b6a8f11', 'GRC-RND-002', 'transaction_count', '4', 'INTEGER', 'COUNT', '7d');