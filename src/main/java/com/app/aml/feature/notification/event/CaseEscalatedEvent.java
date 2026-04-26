package com.app.aml.feature.notification.event;


import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class CaseEscalatedEvent extends ApplicationEvent {

    private final String caseReference;
    private final String adminEmail;
    private final String reason;

    public CaseEscalatedEvent(Object source, String caseReference, String adminEmail, String reason) {
        super(source);
        this.caseReference = caseReference;
        this.adminEmail = adminEmail;
        this.reason = reason;
    }
}