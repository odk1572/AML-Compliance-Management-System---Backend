INSERT INTO common_schema.global_rules (id, rule_name, rule_type, severity, base_risk_score)
VALUES ('01954452-f349-7b2a-a92c-63b71f928e1a', 'Money Mule Funnel Detection (5+ Originators in 7 Days)', 'FUNNEL', 'HIGH', 85);

INSERT INTO common_schema.global_rule_conditions (rule_id, condition_reference, attribute_name, threshold_value, value_data_type, aggregation_function, lookback_period)
VALUES ('01954452-f349-7b2a-a92c-63b71f928e1a', 'GRC-FUN-001', 'originator_account_no', '5', 'INTEGER', 'COUNT', '7d');