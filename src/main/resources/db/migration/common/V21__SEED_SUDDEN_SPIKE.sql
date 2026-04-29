-- Seed the Global Rule for Sudden Spike Detection
INSERT INTO common_schema.global_rules (id, rule_name, rule_type, severity, base_risk_score)
VALUES ('019c00da-c872-7634-8c4d-905e4a3b2c1f', 'Sudden Spike (Recent X hours outflow is > multiple of the x-day daily average)', 'SUDDEN_SPIKE', 'CRITICAL', 90);

-- Seed Condition 1: Recent Activity Window (24h Sum)
INSERT INTO common_schema.global_rule_conditions (rule_id, condition_reference, attribute_name, threshold_value, value_data_type, aggregation_function, lookback_period)
VALUES ('019c00da-c872-7634-8c4d-905e4a3b2c1f', 'GRC-SPK-001', 'recent_activity_window', '0', 'INTEGER', 'SUM', '24h');

-- Seed Condition 2: Historical Baseline Window (30d Average)
INSERT INTO common_schema.global_rule_conditions (rule_id, condition_reference, attribute_name, threshold_value, value_data_type, aggregation_function, lookback_period)
VALUES ('019c00da-c872-7634-8c4d-905e4a3b2c1f', 'GRC-SPK-002', 'historical_baseline_window', '0', 'INTEGER', 'AVG', '30d');

-- Seed Condition 3: Spike Multiplier (Threshold 5.0)
INSERT INTO common_schema.global_rule_conditions (rule_id, condition_reference, attribute_name, threshold_value, value_data_type, aggregation_function)
VALUES ('019c00da-c872-7634-8c4d-905e4a3b2c1f', 'GRC-SPK-003', 'spike_multiplier', '5.0', 'DECIMAL', 'NONE');