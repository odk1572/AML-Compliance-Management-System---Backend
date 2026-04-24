package com.app.aml.feature.platformuser.entity;

import com.app.aml.security.rbac.Role;
import com.app.aml.shared.audit.SoftDeletableEntity;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.domain.Persistable; // Added

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "platform_users", schema = "common_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE common_schema.platform_users SET sys_is_deleted = true, sys_deleted_at = NOW() WHERE id = ?")
@SQLRestriction("sys_is_deleted = false")
public class PlatformUser extends SoftDeletableEntity implements Persistable<UUID> { // Added Persistable

    @Id
    @Builder.Default // Ensure builder uses the generator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @Email
    @NotBlank
    @Size(max = 255)
    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;

    @NotBlank
    @Size(max = 255)
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @NotBlank
    @Size(max = 255)
    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @NotNull
    @Column(name = "is_first_login", nullable = false)
    @Builder.Default
    private boolean isFirstLogin = true;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    @Builder.Default
    private Role role = Role.SUPER_ADMIN;

    @NotNull
    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @NotNull
    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private boolean locked = false;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Size(max = 45)
    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    // --- NEW: Dynamic Persistable Logic ---

    @Transient
    @Builder.Default
    private boolean isNewRecord = true;

    @Override
    @Transient
    public boolean isNew() {
        return isNewRecord;
    }

    @PostLoad
    @PrePersist
    void markNotNew() {
        this.isNewRecord = false;
    }

    @Override
    public UUID getId() {
        return this.id;
    }

    // --- End of Persistable Logic ---

    public void incrementFailedAttempts() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 5) {
            this.lockAccount();
        }
    }

    public void lockAccount() {
        this.locked = true;
        this.lockedAt = Instant.now();
    }

    public void unlockAccount() {
        this.locked = false;
        this.lockedAt = null;
        this.failedLoginAttempts = 0;
    }

    public void recordSuccessfulLogin(String ip) {
        this.lastLoginAt = Instant.now();
        this.lastLoginIp = ip;
        this.failedLoginAttempts = 0;
        // Optionally mark first login complete here if your business logic requires it
        // this.isFirstLogin = false;
    }
}