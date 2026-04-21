package com.app.aml.shared.notification.event;


import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Event published when an AML investigation case is assigned or reassigned.
 * Extends Spring's base ApplicationEvent.
 */
@Getter
public class CaseAssignedEvent extends ApplicationEvent {

    private final String tenantId;
    private final UUID caseId;
    private final String caseReference;
    private final UUID previousAssigneeId; // Can be null if it's the first assignment
    private final UUID newAssigneeId;
    private final String newAssigneeEmail;

    /**
     * @param source             The object on which the event initially occurred (usually 'this' from the publisher).
     * @param tenantId           The ID of the tenant/bank.
     * @param caseId             The database UUID of the case.
     * @param caseReference      The human-readable case reference (e.g., CAS-2026-XYZ).
     * @param previousAssigneeId The UUID of the previous owner (if reassigned).
     * @param newAssigneeId      The UUID of the new owner.
     * @param newAssigneeEmail   The email to send the notification to.
     */
    public CaseAssignedEvent(Object source,
                             String tenantId,
                             UUID caseId,
                             String caseReference,
                             UUID previousAssigneeId,
                             UUID newAssigneeId,
                             String newAssigneeEmail) {
        super(source);
        this.tenantId = tenantId;
        this.caseId = caseId;
        this.caseReference = caseReference;
        this.previousAssigneeId = previousAssigneeId;
        this.newAssigneeId = newAssigneeId;
        this.newAssigneeEmail = newAssigneeEmail;
    }
}