package com.app.aml.feature.tenantuser.dto;

import com.app.aml.security.rbac.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTenantUserRequestDto {

    @NotBlank(message = "Full name is required")
    @Size(max = 255)
    private String fullName;

    @NotNull(message = "Role is required")
    private Role role;

    @NotNull(message = "Lock status is required")
    private Boolean isLocked; // Allows admin to manually lock/unlock an account
}