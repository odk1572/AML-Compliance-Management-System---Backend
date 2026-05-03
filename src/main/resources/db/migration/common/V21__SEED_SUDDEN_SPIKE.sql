INSERT INTO common_schema.global_rules (id, rule_name, rule_type, severity, base_risk_score)
VALUES ('019c00da-c872-7634-8c4d-905e4a3b2c1f', 'Sudden Spike (Recent X hours outflow is > multiple of the x-day daily average)', 'SUDDEN_SPIKE', 'CRITICAL', 90);

INSERT INTO common_schema.global_rule_conditions (rule_id, condition_reference, attribute_name, threshold_value, value_data_type, aggregation_function, lookback_period)
VALUES ('019c00da-c872-7634-8c4d-905e4a3b2c1f', 'GRC-SPK-001', 'recent_activity_window', '0', 'INTEGER', 'SUM', '24h');

INSERT INTO common_schema.global_rule_conditions (rule_id, condition_reference, attribute_name, threshold_value, value_data_type, aggregation_function, lookback_period)
VALUES ('019c00da-c872-7634-8c4d-905e4a3b2c1f', 'GRC-SPK-002', 'historical_baseline_window', '0', 'INTEGER', 'AVG', '30d');

INSERT INTO common_schema.global_rule_conditions (rule_id, condition_reference, attribute_name, threshold_value, value_data_type, aggregation_function)
VALUES ('019c00da-c872-7634-8c4d-905e4a3b2c1f', 'GRC-SPK-003', 'spike_multiplier', '5.0', 'DECIMAL', 'NONE');