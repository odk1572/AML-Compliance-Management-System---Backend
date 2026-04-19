package com.app.aml.multitenency;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenantId(String tenantId) {
        log.debug("Switching context to Tenant ID: {}", tenantId);
        CURRENT_TENANT.set(tenantId);
    }
    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }
    public static void clear() {
        log.debug("Clearing Tenant context for thread: {}", Thread.currentThread().getName());
        CURRENT_TENANT.remove();
    }
}