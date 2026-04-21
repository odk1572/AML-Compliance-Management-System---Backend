package com.app.aml.shared.notification.entity;


import com.app.aml.shared.audit.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "in_platform_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InPlatformNotification extends SoftDeletableEntity {

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "read_at")
    private Instant readAt;

    @Builder
    public InPlatformNotification(UUID id, UUID recipientId, String notificationType, String title, String body) {
        // Assuming your SoftDeletableEntity / BaseEntity has an ID setter or constructor
        super.setId(id);
        this.recipientId = recipientId;
        this.notificationType = notificationType;
        this.title = title;
        this.body = body;
        this.isRead = false;
    }

    /**
     * Helper method to mark the notification as read and record the timestamp.
     */
    public void markAsRead() {
        if (!this.isRead) {
            this.isRead = true;
            this.readAt = Instant.now();
        }
    }
}