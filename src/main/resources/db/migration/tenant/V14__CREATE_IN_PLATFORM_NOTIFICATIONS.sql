CREATE TABLE in_platform_notifications (
                                           id UUID PRIMARY KEY,
                                           recipient_id UUID NOT NULL REFERENCES tenant_users(id) ON DELETE CASCADE,

                                           notification_type VARCHAR(50) NOT NULL,

                                           title VARCHAR(255) NOT NULL,
                                           body TEXT NOT NULL,

                                           is_read BOOLEAN NOT NULL DEFAULT FALSE,
                                           read_at TIMESTAMP,


                                           sys_is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                                           sys_deleted_at TIMESTAMP,
                                           sys_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                           sys_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                           CONSTRAINT chk_notification_type CHECK (notification_type IN (
                                                                                                          'CASE_ASSIGNED',
                                                                                                         'STR_FILED',
                                                                                                         'ESCALATION',
                                                                                                         'BATCH_DONE'
                                               ))
);

CREATE TRIGGER trg_notifications_updated_at
    BEFORE UPDATE ON in_platform_notifications
    FOR EACH ROW
    EXECUTE FUNCTION update_sys_updated_at_column();


-- Critical for the "Unread Count" badge and Notification Bell UI
CREATE INDEX idx_notif_recipient_unread ON in_platform_notifications(recipient_id) WHERE is_read = FALSE;

-- For fetching the notification history for a specific user
CREATE INDEX idx_notif_recipient_created ON in_platform_notifications(recipient_id, sys_created_at DESC);

-- For soft deletion cleanup
CREATE INDEX idx_notif_sys_is_deleted ON in_platform_notifications(sys_is_deleted);