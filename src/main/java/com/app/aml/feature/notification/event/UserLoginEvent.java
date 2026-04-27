package com.app.aml.feature.notification.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.UUID;

@Getter
public class UserLoginEvent extends ApplicationEvent {
    private final String tenantId;
    private final UUID userId;
    private final String userEmail;
    private final String ipAddress;

    public UserLoginEvent(Object source, String tenantId, UUID userId, String userEmail, String ipAddress) {
        super(source);
        this.tenantId = tenantId;
        this.userId = userId;
        this.userEmail = userEmail;
        this.ipAddress = ipAddress;
    }
}