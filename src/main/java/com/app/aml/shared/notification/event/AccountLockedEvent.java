package com.app.aml.shared.notification.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Event published when a user account is locked due to security reasons
 * (e.g., too many failed login attempts).
 */
@Getter
public class AccountLockedEvent extends ApplicationEvent {

    private final String tenantId;
    private final UUID userId;
    private final String userEmail;
    private final String reason;

    /**
     * @param source    The object on which the event initially occurred.
     * @param tenantId  The ID of the tenant (can be NULL for Platform/Super Admins).
     * @param userId    The database UUID of the locked user.
     * @param userEmail The email address where the security alert should be sent.
     * @param reason    A description of why the account was locked (e.g., "EXCESSIVE_FAILED_LOGINS").
     */
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
