package com.app.aml.shared.audit.repository;


import com.app.aml.shared.audit.entity.TenantAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface TenantAuditLogRepository extends JpaRepository<TenantAuditLog, UUID> {

    Page<TenantAuditLog> findByActorId(UUID actorId, Pageable pageable);

    Page<TenantAuditLog> findByActionCategory(String actionCategory, Pageable pageable);

    Page<TenantAuditLog> findByTargetEntityTypeAndTargetEntityId(
            String targetEntityType,
            UUID targetEntityId,
            Pageable pageable
    );

    Page<TenantAuditLog> findBySysCreatedAtBetween(Instant start, Instant end, Pageable pageable);
}