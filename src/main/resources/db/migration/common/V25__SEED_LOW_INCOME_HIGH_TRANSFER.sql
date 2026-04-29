-- Seed the Global Rule for Low Income High Transfer
INSERT INTO common_schema.global_rules (id, rule_name, rule_type, severity, base_risk_score)
VALUES ('019c00bd-9173-774b-b04d-450f789a1c2e', 'Disproportionate Outflow (Transfers > Ax Monthly Income)', 'LOW_INCOME_HIGH_TRANSFER', 'CRITICAL', 95);

-- Seed Condition: Monthly Income Multiplier
INSERT INTO common_schema.global_rule_conditions (rule_id, condition_reference, attribute_name, threshold_value, value_data_type, aggregation_function, lookback_period)
VALUES ('019c00bd-9173-774b-b04d-450f789a1c2e', 'GRC-LIH-001', 'monthly_income_multiplier', '3.0', 'DECIMAL', 'NONE', '30d');