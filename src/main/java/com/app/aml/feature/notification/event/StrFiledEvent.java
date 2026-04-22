package com.app.aml.feature.notification.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class StrFiledEvent extends ApplicationEvent {

    private final String tenantId;
    private final UUID strId;
    private final String strReference;
    private final UUID filedById;
    private final String filedByEmail;

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