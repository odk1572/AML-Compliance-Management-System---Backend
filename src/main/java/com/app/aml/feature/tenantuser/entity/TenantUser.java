package com.app.aml.feature.tenantuser.entity;

import com.app.aml.security.rbac.Role;
import com.app.aml.audit.SoftDeletableEntity;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TenantUser extends SoftDeletableEntity implements Persistable<UUID> {

    @Id
    @Builder.Default
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @NotBlank
    @Size(max = 100)
    @Column(name = "employee_id", unique = true, nullable = false, length = 100)
    private String employeeId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Email
    @NotBlank
    @Size(max = 255)
    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;

    @NotBlank
    @Size(max = 255)
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @NotNull
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private Role role = Role.COMPLIANCE_OFFICER;

    @NotNull
    @Builder.Default
    @Column(name = "is_first_login", nullable = false)
    private boolean isFirstLogin = true;

    @NotNull
    @Builder.Default
    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts = 0;

    @NotNull
    @Builder.Default
    @Column(name = "is_locked", nullable = false)
    private boolean isLocked = false;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Size(max = 45)
    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    /**
     * Self-referencing foreign key as defined in SQL:
     * sys_created_by UUID REFERENCES tenant_users(id)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sys_created_by", referencedColumnName = "id")
    private TenantUser sysCreatedBy;

    // --- DYNAMIC Persistable Logic (Fixes Duplicate Key Error) ---

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

    // --- Helper Methods ---

    public void incrementFailedAttempts() {
        this.failedLoginAttempts++;
    }

    public void lock() {
        this.isLocked = true;
        this.lockedAt = Instant.now();
    }

    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
        this.isLocked = false;
        this.lockedAt = null;
    }

    public void markFirstLoginComplete() {
        this.isFirstLogin = false;
    }
}