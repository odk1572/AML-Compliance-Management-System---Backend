ALTER TABLE global_rules
DROP CONSTRAINT chk_global_rules_type;

ALTER TABLE global_rules
    ADD CONSTRAINT chk_global_rules_type CHECK (
        rule_type IN (
                      'STRUCTURING',
                      'VELOCITY',
                      'LARGE_TRANSACTION',
                      'ROUND_AMOUNT',
                      'PASS_THROUGH',
                      'FUNNEL',
                      'SUDDEN_SPIKE',
                      'DORMANT_REACTIVATION',
                      'LOW_INCOME_HIGH_TRANSFER',
                      'SCATTER'
            )
        );