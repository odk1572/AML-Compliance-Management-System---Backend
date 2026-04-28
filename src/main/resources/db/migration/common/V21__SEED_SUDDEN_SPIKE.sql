-- Seed the Global Rule for Sudden Spike Detection
INSERT INTO common_schema.global_rules (id, rule_name, rule_type, severity, base_risk_score)
VALUES ('019c00da-c872-7634-8c4d-905e4a3b2c1f', 'Sudden Spike (Recent 24h outflow is > 5x the 30-day daily average)', 'SUDDEN_SPIKE', 'CRITICAL', 90);

-- Seed Condition 1: Recent Activity Window (24h Sum)
INSERT INTO common_schema.global_rule_conditions (rule_id, attribute_name, threshold_value, value_data_type, aggregation_function, lookback_period)
VALUES ('019c00da-c872-7634-8c4d-905e4a3b2c1f', 'recent_activity_window', '0', 'INTEGER', 'SUM', '24h');

-- Seed Condition 2: Historical Baseline Window (30d Average)
INSERT INTO common_schema.global_rule_conditions (rule_id, attribute_name, threshold_value, value_data_type, aggregation_function, lookback_period)
VALUES ('019c00da-c872-7634-8c4d-905e4a3b2c1f', 'historical_baseline_window', '0', 'INTEGER', 'AVG', '30d');

-- Seed Condition 3: Spike Multiplier (Threshold 5.0)
INSERT INTO common_schema.global_rule_conditions (rule_id, attribute_name, threshold_value, value_data_type, aggregation_function)
VALUES ('019c00da-c872-7634-8c4d-905e4a3b2c1f', 'spike_multiplier', '5.0', 'DECIMAL', 'NONE');