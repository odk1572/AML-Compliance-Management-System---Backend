INSERT INTO common_schema.global_rules (id, rule_name, rule_type, severity, base_risk_score)
VALUES ('019dc8d4-2969-7884-a327-59d4e2b98d57', 'High Frequency Transaction Monitor', 'VELOCITY', 'MEDIUM', 50);

INSERT INTO common_schema.global_rule_conditions (rule_id, attribute_name, threshold_value, value_data_type, aggregation_function, lookback_period)
VALUES ('019dc8d4-2969-7884-a327-59d4e2b98d57', 'Transaction Count per Hour', '10', 'INTEGER', 'COUNT', '1h');