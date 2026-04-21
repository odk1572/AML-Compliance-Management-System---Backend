package com.app.aml.shared.notification.repository;

import com.app.aml.shared.notification.entity.InPlatformNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InPlatformNotificationRepository extends JpaRepository<InPlatformNotification, UUID> {

    List<InPlatformNotification> findByRecipientIdOrderBySysCreatedAtDesc(UUID recipientId);

    List<InPlatformNotification> findByRecipientIdAndIsReadFalseOrderBySysCreatedAtDesc(UUID recipientId);

    long countByRecipientIdAndIsReadFalse(UUID recipientId);
}