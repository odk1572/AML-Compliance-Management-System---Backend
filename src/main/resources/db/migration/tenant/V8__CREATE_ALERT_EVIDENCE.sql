-- ============================================================
-- TABLE: alert_evidence
-- Refactored for CHANGE 3 OF 4: Immutable Investigation Snapshot
-- ============================================================
CREATE TABLE alert_evidence (
                                id                      UUID            PRIMARY KEY,
                                alert_id                UUID            NOT NULL REFERENCES alerts(id) ON DELETE CASCADE,

    -- Flat immutable snapshot: These columns prevent the need for complex
    -- joins during investigation and preserve the exact logic used at the time of alert.

    -- e.g., 'Single Transaction Limit' or 'Dormant Period'
                                attribute_name          VARCHAR(100)    NOT NULL,

    -- e.g., 'NONE', 'SUM', 'COUNT' (Helps identify the parameter type)
                                aggregation_function    VARCHAR(10)     NOT NULL,

    -- e.g., 'GREATER_THAN', 'LESS_THAN', 'EQUALS'
                                operator                VARCHAR(30)     NOT NULL,

    -- The threshold active at trigger time (either Global default or Tenant override)
                                threshold_applied       VARCHAR(255)    NOT NULL,

    -- The actual value found in the customer's data (e.g., '15000.00' when limit was '10000.00')
                                actual_evaluated_value  VARCHAR(255)    NOT NULL,

                                sys_created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for fast retrieval when loading the Investigation/Evidence view in the UI
CREATE INDEX idx_alert_evidence_alert_id ON alert_evidence(alert_id);