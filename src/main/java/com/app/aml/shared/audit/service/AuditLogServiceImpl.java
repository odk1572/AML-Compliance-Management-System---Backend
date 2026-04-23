package com.app.aml.shared.audit.service;

import com.app.aml.multitenency.TenantContext;
import com.app.aml.shared.audit.entity.PlatformAuditLog;
import com.app.aml.shared.audit.entity.TenantAuditLog;
import com.app.aml.shared.audit.repository.PlatformAuditLogRepository;
import com.app.aml.shared.audit.repository.TenantAuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public void log(UUID actorId, String category, String action, String entityType, UUID entityId, Object prev, Object next) {
        try {
            String tenantId = TenantContext.getTenantId();

            if (tenantId != null) {
                // Route to Tenant Schema
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
            } else {
                // Route to Platform (common_schema)
                logPlatform(actorId, category, action, entityType, entityId, prev, next);
            }
        } catch (Exception ex) {
            // Swallow exception to prevent breaking the main business transaction
            log.error("CRITICAL: Failed to write audit log. Action: {}, EntityId: {}", action, entityId, ex);
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
            log.error("CRITICAL: Failed to write platform audit log. Action: {}, EntityId: {}", action, entityId, ex);
        }
    }

    private String getCurrentUserRoleSafe() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && !authentication.getAuthorities().isEmpty()) {
                return authentication.getAuthorities().iterator().next().getAuthority();
            }
        } catch (Exception e) {
            log.debug("No authenticated role found for audit log.");
        }
        return "SYSTEM";
    }

    // --- Overloaded Convenience Methods (for compatibility with existing Service calls) ---

    @Override
    public void log(String action, String entityId, String details) {
        try {
            UUID parsedEntityId = entityId != null ? UUID.fromString(entityId) : null;
            log(getCurrentUserIdSafe(), "SYSTEM", action, "UNKNOWN", parsedEntityId, null, details);
        } catch (Exception ex) {
            log.error("CRITICAL: Failed to write simplified audit log. Action: {}", action, ex);
        }
    }

    // --- Private Helpers ---

    private String toJsonSafe(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String) {
            return (String) obj; // if it's already a string details payload
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize audit log object to JSON. Falling back to toString().", e);
            return obj.toString();
        }
    }

    private UUID getCurrentUserIdSafe() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof String) {
                return UUID.fromString((String) authentication.getPrincipal());
            }
        } catch (Exception e) {
            log.debug("No authenticated user found for audit log actor.");
        }
        return null;
    }
}
