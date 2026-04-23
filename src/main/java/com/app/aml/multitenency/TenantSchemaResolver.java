package com.app.aml.multitenency;

import com.app.aml.domain.enums.TenantStatus;
import com.app.aml.domain.exceptions.TenantNotFoundException;
import com.app.aml.domain.exceptions.TenantSuspendedException;
import com.app.aml.feature.tenant.entity.Tenant;
import com.app.aml.feature.tenant.repository.TenantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider; // Import this
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class TenantSchemaResolver {

    // 1. Break the cycle using ObjectProvider
    private final ObjectProvider<TenantRepository> tenantRepositoryProvider;
    private final Map<String, String> schemaCache = new ConcurrentHashMap<>();

    // Manually define constructor (Remove @RequiredArgsConstructor)
    public TenantSchemaResolver(ObjectProvider<TenantRepository> tenantRepositoryProvider) {
        this.tenantRepositoryProvider = tenantRepositoryProvider;
    }

    public String resolveSchema(String tenantId) {
        // Special case for platform operations
        if (tenantId == null || tenantId.trim().isEmpty() || tenantId.equals("common_schema")) {
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

    private String fetchAndValidateTenant(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty() || "common_schema".equals(tenantId)) {
            return "common_schema";
        }

        log.debug("Cache miss. Validating tenant status in DB for: {}", tenantId);

        // 2. Logic to handle if we are passing the Schema Name directly instead of a UUID
        // (Happens during the createTenant process)
        if (tenantId.endsWith("_schema")) {
            return tenantId;
        }

        UUID id;
        try {
            id = UUID.fromString(tenantId);
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format: {}", tenantId);
            return "common_schema";
        }

        // 3. Fetch from repository via the Provider
        TenantRepository repository = tenantRepositoryProvider.getIfAvailable();
        if (repository == null) {
            throw new IllegalStateException("TenantRepository is not yet initialized");
        }

        Tenant tenant = repository.findById(id)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        if (!TenantStatus.ACTIVE.name().equals(tenant.getStatus().name())) {
            log.warn("Access denied for Tenant {}. Current status: {}", tenantId, tenant.getStatus());
            throw new TenantSuspendedException("Tenant account is " + tenant.getStatus());
        }

        return tenant.getSchemaName();
    }
}