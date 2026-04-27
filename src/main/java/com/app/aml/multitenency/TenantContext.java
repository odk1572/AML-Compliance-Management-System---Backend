package com.app.aml.multitenency;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();
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

    public static boolean isTenantSet() {
        return CURRENT_TENANT.get() != null && !CURRENT_TENANT.get().isEmpty();
    }

    public static void clear() {
        log.debug("Clearing Tenant and Schema Context for thread: {}", Thread.currentThread().getName());
        CURRENT_TENANT.remove();
        CURRENT_SCHEMA.remove();
    }
}