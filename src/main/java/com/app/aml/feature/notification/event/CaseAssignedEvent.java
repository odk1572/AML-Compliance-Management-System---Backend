package com.app.aml.feature.notification.event;


import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class CaseAssignedEvent extends ApplicationEvent {

    private final String tenantId;
    private final UUID caseId;
    private final String caseReference;
    private final UUID previousAssigneeId; // Can be null if it's the first assignment
    private final UUID newAssigneeId;
    private final String newAssigneeEmail;

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