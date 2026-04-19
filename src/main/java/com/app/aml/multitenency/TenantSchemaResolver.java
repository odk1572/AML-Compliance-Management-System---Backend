package com.app.aml.multitenency;

import com.aml.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves the physical PostgreSQL schema name for a given Tenant ID.
 * Uses an in-memory cache to prevent database hits on every request.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantSchemaResolver {

    private final TenantRepository tenantRepository;

    // Thread-safe map to store the resolved schemas.
    // Key: tenantId (String), Value: schemaName (String)
    private final Map<String, String> schemaCache = new ConcurrentHashMap<>();

    /**
     * Returns the schema name for the current tenant.
     * If not found in cache, it queries the database and caches the result.
     *
     * @param tenantId The UUID of the tenant as a String
     * @return The physical schema name (e.g., "tenant_001_schema")
     */
    public String resolveSchema(String tenantId) {
        if (tenantId == null) {
            // If no tenant context is set, default to the common/platform schema
            return "common_schema";
        }

        // computeIfAbsent is atomic: it only executes the fetch method if the key is missing.
        return schemaCache.computeIfAbsent(tenantId, this::fetchSchemaFromDatabase);
    }

    /**
     * Removes a tenant from the cache.
     * Must be called by the TenantService whenever a tenant's schema or status is updated.
     */
    public void evict(String tenantId) {
        if (tenantId != null) {
            log.info("Evicting schema cache for tenantId: {}", tenantId);
            schemaCache.remove(tenantId);
        }
    }

    /**
     * Database fallback. Only executed on the first request for a specific tenant,
     * or after an eviction.
     */
    private String fetchSchemaFromDatabase(String tenantId) {
        log.debug("Cache miss. Fetching schema name from DB for tenantId: {}", tenantId);

        UUID id = UUID.fromString(tenantId);

        return tenantRepository.findById(id)
                .map(tenant -> tenant.getSchemaName())
                .orElseThrow(() -> new IllegalArgumentException("Tenant ID not found in database: " + tenantId));
    }
}