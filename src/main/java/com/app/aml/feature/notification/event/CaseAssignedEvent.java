package com.app.aml.feature.notification.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class CaseAssignedEvent extends ApplicationEvent {

    private final String caseReference;
    private final String newAssigneeEmail;

    public CaseAssignedEvent(Object source, String caseReference, String newAssigneeEmail) {
        super(source);
        this.caseReference = caseReference;
        this.newAssigneeEmail = newAssigneeEmail;
    }
}