package com.app.aml.feature.notification.event;


import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class TenantCreatedEvent extends ApplicationEvent {

    private final UUID tenantId;
    private final String tenantName;
    private final String schemaName;
    private final String adminEmail;
    private final String bankCode; // e.g., "BNK-123"

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