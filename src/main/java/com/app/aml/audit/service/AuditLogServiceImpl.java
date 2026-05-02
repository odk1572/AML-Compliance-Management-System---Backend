package com.app.aml.audit.service;

import com.app.aml.multitenency.TenantContext;
import com.app.aml.audit.entity.PlatformAuditLog;
import com.app.aml.audit.entity.TenantAuditLog;
import com.app.aml.audit.repository.PlatformAuditLogRepository;
import com.app.aml.audit.repository.TenantAuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final PlatformAuditLogRepository platformRepo;
    private final TenantAuditLogRepository tenantRepo;
    private final ObjectMapper objectMapper;

    public Page<?> getAuditLogs(Pageable pageable) {
        String tenantId = TenantContext.getTenantId();

        if (tenantId != null && !tenantId.equals("common_schema")) {
            log.debug("Fetching tenant-specific audit logs for: {}", tenantId);
            return tenantRepo.findAll(pageable);
        } else {
            log.debug("Fetching global platform audit logs.");
            return platformRepo.findAll(pageable);
        }
    }

    @Override
    public void log(UUID actorId, String category, String action, String entityType, UUID entityId, Object prev, Object next) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals("common_schema")) {
            logTenant(actorId, category, action, entityType, entityId, prev, next);
        } else {
            logPlatform(actorId, category, action, entityType, entityId, prev, next);
        }
    }

    @Override
    public void logPlatform(UUID actorId, String category, String action, String entityType, UUID entityId, Object prev, Object next) {
        try {
            PlatformAuditLog logEntry = PlatformAuditLog.builder()
                    .actorId(actorId != null ? actorId : getCurrentUserIdSafe())
                    .actorRole(getCurrentUserRoleSafe())
                    .actionCategory(category)
                    .actionPerformed(action)
                    .targetEntityType(entityType)
                    .targetEntityId(entityId)
                    .previousState(toJsonSafe(prev))
                    .newState(toJsonSafe(next))
                    .sysCreatedAt(Instant.now())
                    .build();

            platformRepo.save(logEntry);
        } catch (Exception ex) {
            log.error("CRITICAL: Failed to write platform audit log. Action: {}", action, ex);
        }
    }

    @Override
    public void logTenant(UUID actorId, String category, String action, String entityType, UUID entityId, Object prev, Object next) {
        try {
            TenantAuditLog logEntry = TenantAuditLog.builder()
                    .actorId(actorId != null ? actorId : getCurrentUserIdSafe())
                    .actionCategory(category)
                    .actionPerformed(action)
                    .targetEntityType(entityType)
                    .targetEntityId(entityId)
                    .prevState(toJsonSafe(prev))
                    .nextState(toJsonSafe(next))
                    .sysCreatedAt(Instant.now())
                    .build();

            tenantRepo.save(logEntry);
        } catch (Exception ex) {
            log.error("CRITICAL: Failed to write tenant audit log. Action: {}", action, ex);
        }
    }

    @Override
    public void log(String action, String entityId, String details) {
        UUID parsedEntityId = null;
        try {
            if (entityId != null) parsedEntityId = UUID.fromString(entityId);
        } catch (Exception ignored) {}

        log(null, "SYSTEM", action, "SYSTEM_EVENT", parsedEntityId, null, details);
    }


    private String toJsonSafe(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String) return (String) obj;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }

    private UUID getCurrentUserIdSafe() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof String) {
                return UUID.fromString((String) auth.getPrincipal());
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String getCurrentUserRoleSafe() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && !auth.getAuthorities().isEmpty()) {
                return auth.getAuthorities().iterator().next().getAuthority();
            }
        } catch (Exception ignored) {}
        return "SYSTEM";
    }
}