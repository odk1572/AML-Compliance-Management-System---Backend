package com.app.aml.security.rbac;


import java.util.*;

/**
 * Hardcoded registry of Role-to-Permission mappings based on the AML Access Control Matrix.
 * Zero DB hits for maximum performance during the filter chain.
 */
public class RolePermissionRegistry {

    private static final Map<Role, Set<Permission>> REGISTRY = new EnumMap<>(Role.class);

    static {
        // ---------------------------------------------------------
        // 1. SUPER_ADMIN: Platform Management (Strictly Non-PII)
        // ---------------------------------------------------------
        REGISTRY.put(Role.SUPER_ADMIN, EnumSet.of(
                Permission.GLOBAL_RULE_MANAGE,
                Permission.TENANT_ONBOARD,
                Permission.REPORT_CROSS_TENANT,
                Permission.REPORT_SYSTEM_WIDE,
                Permission.CASE_NOTE_READ ,
                Permission.CASE_INVESTIGATE// View only for platform-level audit trails
        ));

        // ---------------------------------------------------------
        // 2. BANK_ADMIN: Tenant Management + Oversight
        // ---------------------------------------------------------
        REGISTRY.put(Role.BANK_ADMIN, EnumSet.of(
                // Admin & Ops Actions
                Permission.BANK_USER_MANAGE,
                Permission.BATCH_UPLOAD,
                Permission.CASE_ASSIGN,
                Permission.REPORT_INSTITUTIONAL,

                // Read Access for Oversight
                Permission.ALERT_READ,
                Permission.CASE_NOTE_READ
        ));

        // ---------------------------------------------------------
        // 3. COMPLIANCE_OFFICER: The Investigator
        // ---------------------------------------------------------
        REGISTRY.put(Role.COMPLIANCE_OFFICER, EnumSet.of(
                // Core Investigation
                Permission.CASE_INVESTIGATE,
                Permission.CASE_NOTE_WRITE,
                Permission.CASE_NOTE_READ,
                Permission.STR_FILE,

                // Context Access
                Permission.ALERT_READ,
                Permission.TRANSACTION_READ
        ));
    }

    public static Set<Permission> getPermissions(Role role) {
        return REGISTRY.getOrDefault(role, Collections.emptySet());
    }

    public static boolean hasPermission(Role role, Permission permission) {
        return getPermissions(role).contains(permission);
    }
}