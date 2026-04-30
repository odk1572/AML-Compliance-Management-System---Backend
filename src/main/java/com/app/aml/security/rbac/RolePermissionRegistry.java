package com.app.aml.security.rbac;


import java.util.*;

public class RolePermissionRegistry {

    private static final Map<Role, Set<Permission>> REGISTRY = new EnumMap<>(Role.class);

    static {
        REGISTRY.put(Role.SUPER_ADMIN, EnumSet.of(
                Permission.GLOBAL_RULE_MANAGE,
                Permission.TENANT_ONBOARD,
                Permission.REPORT_CROSS_TENANT,
                Permission.REPORT_SYSTEM_WIDE,
                Permission.CASE_NOTE_READ ,
                Permission.CASE_INVESTIGATE
        ));

        REGISTRY.put(Role.BANK_ADMIN, EnumSet.of(
                Permission.BANK_USER_MANAGE,
                Permission.BATCH_UPLOAD,
                Permission.CASE_ASSIGN,
                Permission.REPORT_INSTITUTIONAL,
                Permission.ALERT_READ,
                Permission.CASE_NOTE_READ,
                Permission.CASE_INVESTIGATE,
                Permission.GLOBAL_RULE_MANAGE
        ));

        REGISTRY.put(Role.COMPLIANCE_OFFICER, EnumSet.of(
                Permission.CASE_INVESTIGATE,
                Permission.CASE_NOTE_WRITE,
                Permission.CASE_NOTE_READ,
                Permission.STR_FILE,

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