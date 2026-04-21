package com.app.aml.shared.notification.event;


import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Event published when a new Bank/Tenant is onboarded to the platform.
 * Triggers schema provisioning and initial admin setup.
 */
@Getter
public class TenantCreatedEvent extends ApplicationEvent {

    private final UUID tenantId;
    private final String tenantName;
    private final String schemaName;
    private final String adminEmail;
    private final String bankCode; // e.g., "BNK-123"

    /**
     * @param source      The service that handled the onboarding request.
     * @param tenantId    The unique ID generated for the new tenant.
     * @param tenantName  The display name of the bank.
     * @param schemaName  The physical PostgreSQL schema name (e.g., "bank_123_schema").
     * @param adminEmail  The email of the primary Bank Admin.
     * @param bankCode   The unique identifier for the bank's internal reference.
     */
    public TenantCreatedEvent(Object source,
                              UUID tenantId,
                              String tenantName,
                              String schemaName,
                              String adminEmail,
                              String bankCode) {
        super(source);
        this.tenantId = tenantId;
        this.tenantName = tenantName;
        this.schemaName = schemaName;
        this.adminEmail = adminEmail;
        this.bankCode = bankCode;
    }
}