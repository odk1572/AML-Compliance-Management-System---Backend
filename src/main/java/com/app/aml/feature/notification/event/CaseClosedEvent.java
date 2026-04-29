package com.app.aml.feature.notification.event;


import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.UUID;

@Getter
public class CaseClosedEvent extends ApplicationEvent {
    private final String caseReference;
    private final String disposition;
    private final String closedByEmail;

    public CaseClosedEvent(Object source, String caseReference, String disposition, String closedByEmail) {
        super(source);
        this.caseReference = caseReference;
        this.disposition = disposition;
        this.closedByEmail = closedByEmail;
    }
}