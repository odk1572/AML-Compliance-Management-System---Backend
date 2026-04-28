-- Seed the Global Rule for Round Amount Detection
INSERT INTO common_schema.global_rules (id, rule_name, rule_type, severity, base_risk_score)
VALUES ('019c00c8-471a-7b3e-8f5d-2c9e4b6a8f11', 'Frequent Round Amount Transfers (Multiples of $1,000)', 'ROUND_AMOUNT', 'HIGH', 80);

-- Seed Condition 1: Amount Divisor (Multiples of 1000)
INSERT INTO common_schema.global_rule_conditions (rule_id, attribute_name, threshold_value, value_data_type, aggregation_function, lookback_period)
VALUES ('019c00c8-471a-7b3e-8f5d-2c9e4b6a8f11', 'amount_divisor', '1000', 'INTEGER', 'NONE', '7d');

-- Seed Condition 2: Transaction Count (At least 4 times)
INSERT INTO common_schema.global_rule_conditions (rule_id, attribute_name, threshold_value, value_data_type, aggregation_function, lookback_period)
VALUES ('019c00c8-471a-7b3e-8f5d-2c9e4b6a8f11', 'transaction_count', '4', 'INTEGER', 'COUNT', '7d');