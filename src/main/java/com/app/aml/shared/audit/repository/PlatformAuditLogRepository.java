package com.app.aml.shared.audit.repository;


import com.app.aml.shared.audit.entity.PlatformAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PlatformAuditLogRepository extends JpaRepository<PlatformAuditLog, UUID> {

    Page<PlatformAuditLog> findByActorId(UUID actorId, Pageable pageable);

    Page<PlatformAuditLog> findByTenantId(UUID tenantId, Pageable pageable);

    Page<PlatformAuditLog> findByActionCategory(String actionCategory, Pageable pageable);

    Page<PlatformAuditLog> findByTargetEntityTypeAndTargetEntityId(
            String targetEntityType,
            UUID targetEntityId,
            Pageable pageable
    );
}