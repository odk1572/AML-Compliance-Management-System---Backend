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

}