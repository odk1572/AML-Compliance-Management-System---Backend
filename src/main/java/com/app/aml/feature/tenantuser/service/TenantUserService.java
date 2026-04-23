package com.app.aml.feature.tenantuser.service;


import com.app.aml.feature.tenantuser.dto.CreateTenantUserRequestDto;
import com.app.aml.feature.tenantuser.dto.TenantUserResponseDto;
import com.app.aml.feature.tenantuser.dto.UpdateTenantUserRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.app.aml.security.rbac.Role;

import java.util.UUID;

public interface TenantUserService {
    TenantUserResponseDto createComplianceOfficer(CreateTenantUserRequestDto dto);
    void deactivateUser(UUID id);
    void resetPassword(UUID id);
    Page<TenantUserResponseDto> listUsers(Role role, Pageable pageable);
    void changePassword(UUID userId, String oldPassword, String newPassword);
    TenantUserResponseDto updateUser(UUID id, UpdateTenantUserRequestDto dto);
    void unlockUser(UUID id);
    TenantUserResponseDto getUserById(UUID id);
    void reactivateUser(UUID id);
}