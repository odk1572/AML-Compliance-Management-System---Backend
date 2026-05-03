ALTER TABLE alerts RENAME COLUMN transaction_id TO triggering_transaction_id;

CREATE TABLE alert_transactions (
                                    id UUID PRIMARY KEY,
                                    alert_id UUID NOT NULL REFERENCES alerts(id) ON DELETE CASCADE,
                                    transaction_id UUID NOT NULL REFERENCES transactions(id) ON DELETE RESTRICT,

                                    involvement_role VARCHAR(50) DEFAULT 'CONTRIBUTOR',

                                    sys_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                    CONSTRAINT uk_alert_transaction_link UNIQUE (alert_id, transaction_id)
);

CREATE INDEX idx_at_alert_id ON alert_transactions(alert_id);
CREATE INDEX idx_at_transaction_id ON alert_transactions(transaction_id);