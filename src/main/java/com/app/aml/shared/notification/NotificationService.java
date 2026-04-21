package com.app.aml.shared.notification;


import com.app.aml.shared.notification.entity.InPlatformNotification;
import com.app.aml.shared.notification.repository.InPlatformNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Central service for dispatching all user notifications.
 * Prioritizes in-platform delivery and offloads external events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final InPlatformNotificationRepository notifRepo;
    private final ApplicationEventPublisher publisher;

    @Transactional
    public void createInPlatform(UUID recipientId, String type, String title, String body) {
        log.debug("Creating in-platform notification for user: {} (Type: {})", recipientId, type);

        InPlatformNotification notification = InPlatformNotification.builder()
                .recipientId(recipientId)
                .notificationType(type)
                .title(title)
                .body(body)
                .build();
        try {
            notifRepo.save(notification);
        } catch (Exception e) {
            log.error("Critical Failure: Could not save in-platform notification for user {}", recipientId, e);
            // We rethrow here because if the DB fails, the caller (Service)
            // might need to know for transaction rollback purposes.
            throw e;
        }
    }

    public void publishEvent(ApplicationEvent event) {
        log.debug("Publishing notification event: {}", event.getClass().getSimpleName());

        publisher.publishEvent(event);
    }

    @Transactional(readOnly = true)
    public List<InPlatformNotification> getUserNotifications(UUID recipientId) {
        log.debug("Fetching notifications for user: {}", recipientId);
        return notifRepo.findByRecipientIdOrderBySysCreatedAtDesc(recipientId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID recipientId) {
        return notifRepo.countByRecipientIdAndIsReadFalse(recipientId);
    }

    @Transactional
    public void markAsRead(UUID notificationId, UUID currentUserId) {
        log.debug("Marking notification {} as read for user {}", notificationId, currentUserId);

        InPlatformNotification notification = notifRepo.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        if (!notification.getRecipientId().equals(currentUserId)) {
            throw new SecurityException("You do not have permission to modify this notification.");
        }

        notification.markAsRead(); // This uses the helper method we added to the entity
        notifRepo.save(notification);
    }

    @Transactional
    public void markAllAsRead(UUID recipientId) {
        log.debug("Marking all notifications as read for user {}", recipientId);

        List<InPlatformNotification> unreadNotifs = notifRepo
                .findByRecipientIdAndIsReadFalseOrderBySysCreatedAtDesc(recipientId);

        unreadNotifs.forEach(InPlatformNotification::markAsRead);

        notifRepo.saveAll(unreadNotifs);
    }
}