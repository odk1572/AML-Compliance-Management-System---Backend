package com.app.aml.feature.notification.entity;


import com.app.aml.audit.SoftDeletableEntity;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "in_platform_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InPlatformNotification extends SoftDeletableEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

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
        this.id=id;
        this.recipientId = recipientId;
        this.notificationType = notificationType;
        this.title = title;
        this.body = body;
        this.isRead = false;
    }


    public void markAsRead() {
        if (!this.isRead) {
            this.isRead = true;
            this.readAt = Instant.now();
        }
    }
    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UuidCreator.getTimeOrderedEpoch();
        }
    }

}