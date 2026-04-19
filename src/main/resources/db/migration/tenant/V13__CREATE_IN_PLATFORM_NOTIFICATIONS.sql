
CREATE TABLE in_platform_notifications (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),

    -- Recipient (CO or Bank Admin)
    recipient_id        UUID            NOT NULL,

    notification_type   VARCHAR(20)     NOT NULL,
    title               VARCHAR(200)    NOT NULL,
    body                TEXT            NOT NULL,

    -- Read state
    is_read             BOOLEAN         NOT NULL DEFAULT FALSE,
    read_at             TIMESTAMPTZ,

    -- Soft delete — user dismissal
    sys_is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    sys_deleted_at      TIMESTAMPTZ,

    -- Audit timestamps
    sys_created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    sys_updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- ── Constraints ──────────────────────────────────────────
    CONSTRAINT pk_in_platform_notifications
        PRIMARY KEY (id),

    CONSTRAINT chk_notif_type
        CHECK (notification_type IN (
            'CASE_ASSIGNED',
            'CASE_REASSIGNED',
            'STR_FILED',
            'ESCALATION',
            'BATCH_DONE',
            'BATCH_FAILED',
            'ACCOUNT_LOCKED'
        )),

    CONSTRAINT chk_notif_title_not_empty
        CHECK (length(trim(title)) > 0),

    -- read_at must be set when is_read = TRUE
    CONSTRAINT chk_notif_read_at
        CHECK (
            is_read = FALSE OR read_at IS NOT NULL
        ),

    CONSTRAINT fk_notif_recipient
        FOREIGN KEY (recipient_id)
        REFERENCES tenant_users (id)
);

-- ── Indexes ───────────────────────────────────────────────────
-- Angular notification inbox — unread notifications for current user
-- This is the primary query pattern — hits on every page load
CREATE INDEX idx_notif_recipient_unread
    ON in_platform_notifications (recipient_id, sys_created_at DESC)
    WHERE is_read = FALSE AND sys_is_deleted = FALSE;

-- Full inbox view (read + unread, not dismissed)
CREATE INDEX idx_notif_recipient_all
    ON in_platform_notifications (recipient_id, sys_created_at DESC)
    WHERE sys_is_deleted = FALSE;

-- Cleanup job: find old dismissed notifications for archival (90-day window)
CREATE INDEX idx_notif_deleted_time
    ON in_platform_notifications (sys_deleted_at)
    WHERE sys_is_deleted = TRUE;
