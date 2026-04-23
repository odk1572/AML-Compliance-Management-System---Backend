package com.app.aml.shared.audit.service;

import java.util.UUID;

public interface AuditLogService {

    // Smart routing based on TenantContext
    void log(UUID actorId, String category, String action, String entityType, UUID entityId, Object prev, Object next);

    // Explicitly for platform (common_schema)
    void logPlatform(UUID actorId, String category, String action, String entityType, UUID entityId, Object prev, Object next);

    // Explicitly for tenant (tenant_schema)
    void logTenant(UUID actorId, String category, String action, String entityType, UUID entityId, Object prev, Object next);

    // Legacy/System convenience method
    void log(String action, String entityId, String details);
}