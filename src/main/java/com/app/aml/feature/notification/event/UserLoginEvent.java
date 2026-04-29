package com.app.aml.feature.notification.event;


import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.UUID;

@Getter
public class UserLoginEvent extends ApplicationEvent {
    private final UUID userId;
    private final String email;
    private final String ipAddress;
    private final String tenantCode;

    public UserLoginEvent(Object source, UUID userId, String email, String ipAddress, String tenantCode) {
        super(source);
        this.userId = userId;
        this.email = email;
        this.ipAddress = ipAddress;
        this.tenantCode = tenantCode;
    }
}