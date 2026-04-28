-- Seed the Global Rule
INSERT INTO common_schema.global_rules (id, rule_name, rule_type, severity, base_risk_score)
VALUES ('0195441b-4d7a-7589-9a2e-4b47604f8e6b', 'Suspicious Structuring (Under Threshold Smurfing)', 'STRUCTURING', 'HIGH', 85);

-- Seed Condition 1: Individual Ceiling (with Lookback)
INSERT INTO common_schema.global_rule_conditions (rule_id, attribute_name, threshold_value, value_data_type, aggregation_function, lookback_period)
VALUES ('0195441b-4d7a-7589-9a2e-4b47604f8e6b', 'Individual Transaction Ceiling', '10000', 'DECIMAL', 'NONE', '7 days');

-- Seed Condition 2: Cumulative Amount
INSERT INTO common_schema.global_rule_conditions (rule_id, attribute_name, threshold_value, value_data_type, aggregation_function)
VALUES ('0195441b-4d7a-7589-9a2e-4b47604f8e6b', 'Total Cumulative Amount', '50000', 'DECIMAL', 'SUM');

-- Seed Condition 3: Split Count
INSERT INTO common_schema.global_rule_conditions (rule_id, attribute_name, threshold_value, value_data_type, aggregation_function)
VALUES ('0195441b-4d7a-7589-9a2e-4b47604f8e6b', 'Minimum Split Count', '5', 'INTEGER', 'COUNT');