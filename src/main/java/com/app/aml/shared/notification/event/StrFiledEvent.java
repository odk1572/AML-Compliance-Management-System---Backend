package com.app.aml.shared.notification.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Event published when a Suspicious Transaction Report (STR) is officially
 * filed with the regulator.
 */
@Getter
public class StrFiledEvent extends ApplicationEvent {

    private final String tenantId;
    private final UUID strId;
    private final String strReference;
    private final UUID filedById;
    private final String filedByEmail;

    /**
     * @param source        The object on which the event initially occurred.
     * @param tenantId      The ID of the tenant/bank.
     * @param strId         The database UUID of the STR record.
     * @param strReference  The official reference number (e.g., STR-2026-0001).
     * @param filedById     The UUID of the Compliance Officer who performed the filing.
     * @param filedByEmail  The email of the filer for confirmation notifications.
     */
    public StrFiledEvent(Object source,
                         String tenantId,
                         UUID strId,
                         String strReference,
                         UUID filedById,
                         String filedByEmail) {
        super(source);
        this.tenantId = tenantId;
        this.strId = strId;
        this.strReference = strReference;
        this.filedById = filedById;
        this.filedByEmail = filedByEmail;
    }
}