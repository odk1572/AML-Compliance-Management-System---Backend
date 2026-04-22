package com.app.aml.feature.notification.service.impl;

import com.app.aml.feature.notification.dto.BulkMarkAsReadRequestDto;
import com.app.aml.feature.notification.dto.InPlatformNotificationResponseDto;
import com.app.aml.feature.notification.entity.InPlatformNotification;
import com.app.aml.feature.notification.mapper.InPlatformNotificationMapper;
import com.app.aml.feature.notification.repository.InPlatformNotificationRepository;
import com.app.aml.feature.notification.service.interfaces.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final InPlatformNotificationRepository notifRepo;
    private final InPlatformNotificationMapper mapper; // Added Mapper
    private final ApplicationEventPublisher publisher;

    @Transactional
    public void createInPlatform(UUID recipientId, String type, String title, String body) {
        log.info("Dispatching notification to user {}: [{}]", recipientId, title);

        InPlatformNotification notification = InPlatformNotification.builder()
                .recipientId(recipientId)
                .notificationType(type)
                .title(title)
                .body(body)
                .build();

        notifRepo.save(notification);
    }

    public void publishEvent(ApplicationEvent event) {
        log.trace("Publishing notification event: {}", event.getClass().getSimpleName());
        publisher.publishEvent(event);
    }

    @Transactional(readOnly = true)
    public List<InPlatformNotificationResponseDto> getUserNotifications(UUID recipientId) {
        List<InPlatformNotification> notifications = notifRepo.findByRecipientIdOrderBySysCreatedAtDesc(recipientId);
        return mapper.toResponseDtoList(notifications);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID recipientId) {
        return notifRepo.countByRecipientIdAndIsReadFalse(recipientId);
    }

    @Transactional
    public void markAsRead(UUID notificationId, UUID currentUserId) {
        InPlatformNotification notification = notifRepo.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found")); // Replace with custom ResourceNotFoundException

        if (!notification.getRecipientId().equals(currentUserId)) {
            log.warn("Unauthorized attempt by user {} to read notification {}", currentUserId, notificationId);
            throw new SecurityException("Access Denied: Notification ownership mismatch.");
        }

        notification.markAsRead();
        notifRepo.save(notification);
    }

    @Transactional
    public void markAllAsRead(UUID recipientId) {
        log.info("Marking all notifications as read for user {}", recipientId);
        notifRepo.markAllAsReadByRecipientId(recipientId, Instant.now());
    }

    @Transactional
    public void markSelectedAsRead(BulkMarkAsReadRequestDto dto, UUID currentUserId) {
        log.debug("Bulk marking {} notifications as read", dto.getNotificationIds().size());
        notifRepo.markAsReadByIds(dto.getNotificationIds(), Instant.now());
    }
}