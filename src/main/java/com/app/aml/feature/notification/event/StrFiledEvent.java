package com.app.aml.feature.notification.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class StrFiledEvent extends ApplicationEvent {

    private final String strReference;
    private final String filedByEmail;

    public StrFiledEvent(Object source, String strReference, String filedByEmail) {
        super(source);
        this.strReference = strReference;
        this.filedByEmail = filedByEmail;
    }
}