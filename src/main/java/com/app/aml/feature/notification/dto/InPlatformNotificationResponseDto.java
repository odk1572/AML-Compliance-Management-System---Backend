package com.app.aml.feature.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InPlatformNotificationResponseDto {

    private UUID id;

    private UUID recipientId;

    private String notificationType;

    private String title;

    private String body;

    private boolean isRead;

    private Instant readAt;

    private Instant sysCreatedAt; // Important for the UI to show "5 minutes ago"
}