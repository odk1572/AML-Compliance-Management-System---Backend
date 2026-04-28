-- Seed the Global Rule for Large Transactions
INSERT INTO common_schema.global_rules (id, rule_name, rule_type, severity, base_risk_score)
VALUES ('019c00b2-3f8a-7d4e-9b2c-1a2b3c4d5e6f', 'Large Transaction Alert (>$10,000)', 'LARGE_TRANSACTION', 'HIGH', 80);

-- Seed Condition: Transaction Amount Threshold
INSERT INTO common_schema.global_rule_conditions (rule_id, attribute_name, threshold_value, value_data_type, aggregation_function, lookback_period)
VALUES ('019c00b2-3f8a-7d4e-9b2c-1a2b3c4d5e6f', 'amount', '10000.00', 'DECIMAL', 'NONE', '24h');