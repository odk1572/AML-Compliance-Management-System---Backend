package com.app.aml.shared.notification.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;


@Getter
public class AccountLockedEvent extends ApplicationEvent {

    private final String tenantId;
    private final UUID userId;
    private final String userEmail;
    private final String reason;

    public AccountLockedEvent(Object source,
                              String tenantId,
                              UUID userId,
                              String userEmail,
                              String reason) {
        super(source);
        this.tenantId = tenantId;
        this.userId = userId;
        this.userEmail = userEmail;
        this.reason = reason;
    }
}
