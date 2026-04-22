package com.app.aml.feature.notification.repository;

import com.app.aml.feature.notification.entity.InPlatformNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface InPlatformNotificationRepository extends JpaRepository<InPlatformNotification, UUID> {

    List<InPlatformNotification> findByRecipientIdOrderBySysCreatedAtDesc(UUID recipientId);

    List<InPlatformNotification> findByRecipientIdAndIsReadFalseOrderBySysCreatedAtDesc(UUID recipientId);

    long countByRecipientIdAndIsReadFalse(UUID recipientId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE InPlatformNotification n SET n.isRead = true, n.readAt = :now " +
            "WHERE n.recipientId = :recipientId AND n.isRead = false")
    void markAllAsReadByRecipientId(@Param("recipientId") UUID recipientId, @Param("now") Instant now);


    @Modifying(clearAutomatically = true)
    @Query("UPDATE InPlatformNotification n SET n.isRead = true, n.readAt = :now " +
            "WHERE n.id IN :ids")
    void markAsReadByIds(@Param("ids") List<UUID> ids, @Param("now") Instant now);
}