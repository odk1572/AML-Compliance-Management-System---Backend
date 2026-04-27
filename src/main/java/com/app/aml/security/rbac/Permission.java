package com.app.aml.security.rbac;

/**
 * Granular permissions mapped strictly to the AML System Access Control matrix.
 */
public enum Permission {

    GLOBAL_RULE_MANAGE,
    TENANT_ONBOARD,
    REPORT_CROSS_TENANT,
    REPORT_SYSTEM_WIDE,
    BANK_USER_MANAGE,
    BATCH_UPLOAD,
    CASE_ASSIGN,
    REPORT_INSTITUTIONAL,
    CASE_INVESTIGATE,
    CASE_NOTE_WRITE,
    STR_FILE,
    ALERT_READ,
    TRANSACTION_READ,
    CASE_NOTE_READ
}