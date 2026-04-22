package com.app.aml.feature.notification.service.interfaces;

import com.app.aml.feature.notification.dto.BulkMarkAsReadRequestDto;
import com.app.aml.feature.notification.dto.InPlatformNotificationResponseDto;
import org.springframework.context.ApplicationEvent;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    void createInPlatform(UUID recipientId, String type, String title, String body);
    void publishEvent(ApplicationEvent event);
    List<InPlatformNotificationResponseDto> getUserNotifications(UUID recipientId);
    long getUnreadCount(UUID recipientId);
    void markAsRead(UUID notificationId, UUID currentUserId);
    void markAllAsRead(UUID recipientId);
    void markSelectedAsRead(BulkMarkAsReadRequestDto dto, UUID currentUserId);
}
