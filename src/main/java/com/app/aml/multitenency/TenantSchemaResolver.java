package com.app.aml.multitenency;

import com.app.aml.enums.TenantStatus;
import com.app.aml.exceptions.TenantNotFoundException;
import com.app.aml.exceptions.TenantSuspendedException;
import com.app.aml.feature.tenant.entity.Tenant;
import com.app.aml.feature.tenant.repository.TenantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class TenantSchemaResolver {

    private final ObjectProvider<TenantRepository> tenantRepositoryProvider;
    private final Map<String, String> schemaCache = new ConcurrentHashMap<>();

    public TenantSchemaResolver(ObjectProvider<TenantRepository> tenantRepositoryProvider) {
        this.tenantRepositoryProvider = tenantRepositoryProvider;
    }

    public String resolveSchema(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty() || tenantId.equals("common_schema")) {
            return "common_schema";
        }

        String schemaName = schemaCache.get(tenantId);
        if (schemaName != null) {
            return schemaName;
        }

        schemaName = fetchAndValidateTenant(tenantId);

        schemaCache.put(tenantId, schemaName);

        return schemaName;
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

        String originalContext = TenantContext.getTenantId();
        TenantContext.clear();

        try {
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

        } finally {
            if (originalContext != null) {
                TenantContext.setTenantId(originalContext);
            }
        }
    }
}