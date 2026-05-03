CREATE TABLE alert_evidence (
                                id                      UUID            PRIMARY KEY,
                                alert_id                UUID            NOT NULL REFERENCES alerts(id) ON DELETE CASCADE,


                                attribute_name          VARCHAR(100)    NOT NULL,

                                aggregation_function    VARCHAR(10)     NOT NULL,

                                operator                VARCHAR(30)     NOT NULL,

                                threshold_applied       VARCHAR(255)    NOT NULL,

                                actual_evaluated_value  VARCHAR(255)    NOT NULL,

                                sys_created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_alert_evidence_alert_id ON alert_evidence(alert_id);