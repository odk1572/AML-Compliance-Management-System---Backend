package com.app.aml.feature.tenantuser.entity;

import com.app.aml.domain.enums.Role;
import com.app.aml.shared.audit.SoftDeletableEntity;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_users")
@Getter
@Setter
@NoArgsConstructor
public class TenantUser extends SoftDeletableEntity {

    @Id
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
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private Role role = Role.COMPLIANCE_OFFICER;

    @NotNull
    @Column(name = "is_first_login", nullable = false)
    private boolean isFirstLogin = true;

    @NotNull
    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts = 0;

    @NotNull
    @Column(name = "is_locked", nullable = false)
    private boolean isLocked = false;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Size(max = 45)
    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    @Column(name = "sys_created_by")
    private UUID sysCreatedBy;
}