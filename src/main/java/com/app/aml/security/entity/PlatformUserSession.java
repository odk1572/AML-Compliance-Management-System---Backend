package com.app.aml.security.entity;

import com.app.aml.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "platform_user_sessions", schema = "common_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformUserSession extends AuditableEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId; // References PLATFORM_USERS(id)

    @Column(name = "jwt_jti", nullable = false, unique = true)
    private String jwtJti;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "is_revoked", nullable = false)
    @Builder.Default
    private boolean isRevoked = false;

    @Column(name = "revoked_at")
    private Instant revokedAt;
}