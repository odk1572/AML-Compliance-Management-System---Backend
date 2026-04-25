CREATE TABLE alert_evidence (
                                id UUID PRIMARY KEY,
                                alert_id UUID NOT NULL REFERENCES alerts(id) ON DELETE CASCADE,

    -- Snapshot of the rule logic at the moment of the breach
                                threshold_applied VARCHAR(255) NOT NULL,
                                actual_evaluated_value VARCHAR(255) NOT NULL,

                                sys_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_alert_evidence_alert_id ON alert_evidence(alert_id);
