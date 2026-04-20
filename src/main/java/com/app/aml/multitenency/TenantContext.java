package com.app.aml.multitenency;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-local storage for the current request's Tenant ID.
 * Acts as the "source of truth" for the TenantAwareDataSource.
 */
@Slf4j
public class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenantId(String tenantId) {
        log.debug("Setting TenantContext to: {}", tenantId);
        CURRENT_TENANT.set(tenantId);
    }

    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    /**
     * Helper to check if the current request is bound to a specific tenant.
     * If false, the system assumes a 'Global/Super Admin' context.
     */
    public static boolean isTenantSet() {
        return CURRENT_TENANT.get() != null && !CURRENT_TENANT.get().isEmpty();
    }

    /**
     * CRITICAL: Clears the thread-local variable.
     * Must be called at the end of every request to prevent cross-tenant data leakage
     * when threads are returned to the pool.
     */
    public static void clear() {
        log.debug("Clearing TenantContext for thread: {}", Thread.currentThread().getName());
        CURRENT_TENANT.remove();
    }
}