package com.app.aml.security.rbac;

/**
 * Granular permissions mapped strictly to the AML System Access Control matrix.
 */
public enum Permission {

    // ==========================================
    // SYSTEM ADMIN (Platform Level)
    // ==========================================
    GLOBAL_RULE_MANAGE,      // Rule Engine Management
    TENANT_ONBOARD,          // Bank Onboarding
    REPORT_CROSS_TENANT,     // Cross-Tenant Reports
    REPORT_SYSTEM_WIDE,
    // System-Wide Reports

    // ==========================================
    // BANK ADMIN (Tenant Level)
    // ==========================================
    BANK_USER_MANAGE,        // User Management (COs)
    BATCH_UPLOAD,            // Transaction Batch Upload
    CASE_ASSIGN,             // Case Assignment
    REPORT_INSTITUTIONAL,    // Institutional Reports

    // ==========================================
    // COMPLIANCE OFFICER (Investigation Level)
    // ==========================================
    CASE_INVESTIGATE,        // Case Investigation
    CASE_NOTE_WRITE,         // Case Notes (Add Notes)
    STR_FILE,                // SAR/STR Filing

    // ==========================================
    // SHARED / READ-ONLY ACCESS
    // ==========================================
    ALERT_READ,              // Alert Dashboard (View Only)
    TRANSACTION_READ,        // Historical Transaction Review
    CASE_NOTE_READ           // Case Notes / Audit Trail (View Only)
}