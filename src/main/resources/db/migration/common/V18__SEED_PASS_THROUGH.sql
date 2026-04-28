-- Seed the Global Rule for Pass-Through / Layering Detection
INSERT INTO common_schema.global_rules (id, rule_name, rule_type, severity, base_risk_score)
VALUES ('019c00c3-c07a-77e8-b64d-58a4729f3c1e', 'Pass-Through / Layering Detection (5% Margin)', 'PASS_THROUGH', 'CRITICAL', 95);

-- Seed Condition: Balance Margin Percentage
INSERT INTO common_schema.global_rule_conditions (rule_id, attribute_name, threshold_value, value_data_type, aggregation_function, lookback_period)
VALUES ('019c00c3-c07a-77e8-b64d-58a4729f3c1e', 'balance_margin_percentage', '0.05', 'DECIMAL', 'NONE', '24h');