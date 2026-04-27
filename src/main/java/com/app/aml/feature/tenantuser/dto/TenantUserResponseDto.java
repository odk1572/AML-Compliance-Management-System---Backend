package com.app.aml.feature.tenantuser.dto;

import com.app.aml.security.rbac.Role;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TenantUserResponseDto {
    private UUID id;
    private String employeeId;
    private String fullName;
    private String email;
    private Role role;
    private boolean isFirstLogin;
    private boolean isLocked;
    private Instant lastLoginAt;
    private String lastLoginIp;
    private Instant sysCreatedAt;
    private Instant sysUpdatedAt;
}