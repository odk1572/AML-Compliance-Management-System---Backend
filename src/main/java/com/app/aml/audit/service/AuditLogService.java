package com.app.aml.audit.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AuditLogService {

    void log(UUID actorId, String category, String action, String entityType, UUID entityId, Object prev, Object next);

    void logPlatform(UUID actorId, String category, String action, String entityType, UUID entityId, Object prev, Object next);

    void logTenant(UUID actorId, String category, String action, String entityType, UUID entityId, Object prev, Object next);

    void log(String action, String entityId, String details);

    Page<?> getAuditLogs(Pageable pageable);
}