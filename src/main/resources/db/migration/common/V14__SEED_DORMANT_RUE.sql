-- Seed the Global Rule for Dormant Account Reactivation
INSERT INTO common_schema.global_rules (id, rule_name, rule_type, severity, base_risk_score)
VALUES ('01954425-a11b-74f7-8735-8664161f3647', 'Dormant Account Reactivation (High Value)', 'DORMANT_REACTIVATION', 'CRITICAL', 85);

-- Seed Condition 1: Silence Period (Dormancy)
INSERT INTO common_schema.global_rule_conditions (rule_id, attribute_name, threshold_value, value_data_type, aggregation_function, lookback_period)
VALUES ('01954425-a11b-74f7-8735-8664161f3647', 'Silence Period (Dormancy)', '0', 'STRING', 'MIN', '180 days');

-- Seed Condition 2: Detection Window (Reactivation)
INSERT INTO common_schema.global_rule_conditions (rule_id, attribute_name, threshold_value, value_data_type, aggregation_function, lookback_period)
VALUES ('01954425-a11b-74f7-8735-8664161f3647', 'Detection Window (Reactivation)', '0', 'STRING', 'MAX', '7 days');

-- Seed Condition 3: Reactivation Amount Threshold
INSERT INTO common_schema.global_rule_conditions (rule_id, attribute_name, threshold_value, value_data_type, aggregation_function)
VALUES ('01954425-a11b-74f7-8735-8664161f3647', 'Reactivation Amount Threshold', '5000', 'DECIMAL', 'NONE');