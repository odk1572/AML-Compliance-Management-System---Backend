package com.app.aml.multitenency;

import com.app.aml.domain.enums.TenantStatus;
import com.app.aml.domain.exceptions.TenantNotFoundException;
import com.app.aml.domain.exceptions.TenantSuspendedException; // New Exception
import com.app.aml.tenant.entity.Tenant;
import com.app.aml.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantSchemaResolver {

    private final TenantRepository tenantRepository;

    // Cache to store the schema name.
    // Key: tenantId, Value: schemaName
    private final Map<String, String> schemaCache = new ConcurrentHashMap<>();

    public String resolveSchema(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            return "common_schema";
        }

        return schemaCache.computeIfAbsent(tenantId, this::fetchAndValidateTenant);
    }

    public void evict(String tenantId) {
        if (tenantId != null) {
            log.info("Evicting schema cache for tenantId: {}", tenantId);
            schemaCache.remove(tenantId);
        }
    }

    /**
     * Fetches the tenant, but also validates that they are ACTIVE.
     * This is the "gatekeeper" logic for the entire platform.
     */
    private String fetchAndValidateTenant(String tenantId) {
        log.debug("Cache miss. Validating tenant status in DB for: {}", tenantId);

        UUID id;
        try {
            id = UUID.fromString(tenantId);
        } catch (IllegalArgumentException e) {
            throw new TenantNotFoundException("Invalid Tenant ID format: " + tenantId);
        }

        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        // SECURITY GATE: Prevent routing if the bank is SUSPENDED or DELETED
        if (!TenantStatus.ACTIVE.name().equals(tenant.getStatus())) {
            log.warn("Access denied for Tenant {}. Current status: {}",
                    tenantId, tenant.getStatus());
            throw new TenantSuspendedException("Tenant account is " + tenant.getStatus());
        }

        return tenant.getSchemaName();
    }
}