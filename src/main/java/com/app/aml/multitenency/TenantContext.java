package com.app.aml.multitenency;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-local storage for the current request's Tenant ID and Schema Name.
 * Acts as the "source of truth" for the TenantAwareDataSource.
 */
@Slf4j
public class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();
    // FIXED: Added missing ThreadLocal for schema name
    private static final ThreadLocal<String> CURRENT_SCHEMA = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenantId(String tenantId) {
        log.debug("Setting TenantContext to: {}", tenantId);
        CURRENT_TENANT.set(tenantId);
    }

    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void setSchemaName(String schemaName) {
        log.debug("Setting SchemaName to: {}", schemaName);
        CURRENT_SCHEMA.set(schemaName);
    }

    public static String getSchemaName() {
        return CURRENT_SCHEMA.get();
    }

    /**
     * Helper to check if the current request is bound to a specific tenant.
     */
    public static boolean isTenantSet() {
        return CURRENT_TENANT.get() != null && !CURRENT_TENANT.get().isEmpty();
    }

    /**
     * CRITICAL: Clears the thread-local variables.
     * Prevents cross-tenant data leakage when threads are returned to the pool.
     */
    public static void clear() {
        log.debug("Clearing Tenant and Schema Context for thread: {}", Thread.currentThread().getName());
        CURRENT_TENANT.remove();
        CURRENT_SCHEMA.remove(); // FIXED: Now also clears the schema
    }
}