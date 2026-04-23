package com.app.aml.feature.tenant.service;

import com.app.aml.feature.tenant.dto.request.CreateTenantRequestDto;
import com.app.aml.feature.tenant.dto.request.UpdateTenantRequestDto;
import com.app.aml.feature.tenant.dto.response.TenantResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TenantService {
    TenantResponseDto createTenant(CreateTenantRequestDto requestDto);
    TenantResponseDto updateTenant(UUID id, UpdateTenantRequestDto requestDto);
    void deactivateTenant(UUID id);
    Page<TenantResponseDto> listTenants(Pageable pageable);
    TenantResponseDto getTenant(UUID id);
    TenantResponseDto getTenantByCode(String tenantCode);
    TenantResponseDto reactivateTenant(UUID id);
    boolean isTenantCodeAvailable(String tenantCode);
    boolean isSchemaNameAvailable(String schemaName);
    void resetBankAdminCredentials(UUID id);
}