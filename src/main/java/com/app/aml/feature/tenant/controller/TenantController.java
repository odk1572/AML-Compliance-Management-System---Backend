package com.app.aml.feature.tenant.controller;

import com.app.aml.apiResponse.ApiResponse;
import com.app.aml.feature.tenant.dto.request.CreateTenantRequestDto;
import com.app.aml.feature.tenant.dto.request.UpdateTenantRequestDto;
import com.app.aml.feature.tenant.dto.response.TenantResponseDto;
import com.app.aml.feature.tenant.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/platform/tenants")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    public ResponseEntity<ApiResponse<TenantResponseDto>> createTenant(
            @Valid @RequestBody CreateTenantRequestDto requestDto,
            HttpServletRequest request) {

        TenantResponseDto responseDto = tenantService.createTenant(requestDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.of(HttpStatus.CREATED, "Tenant provisioned successfully", request.getRequestURI(), responseDto));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TenantResponseDto>>> listTenants(
            @PageableDefault(size = 20) Pageable pageable,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Tenants retrieved", request.getRequestURI(), tenantService.listTenants(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TenantResponseDto>> getTenant(
            @PathVariable UUID id,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Tenant retrieved", request.getRequestURI(), tenantService.getTenant(id)));
    }

    @GetMapping("/by-code/{code}")
    public ResponseEntity<ApiResponse<TenantResponseDto>> getTenantByCode(
            @PathVariable String code,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Tenant retrieved", request.getRequestURI(), tenantService.getTenantByCode(code)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<TenantResponseDto>> updateTenant(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTenantRequestDto requestDto,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Tenant updated", request.getRequestURI(), tenantService.updateTenant(id, requestDto)));
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<TenantResponseDto>> reactivateTenant(
            @PathVariable UUID id,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Tenant reactivated", request.getRequestURI(), tenantService.reactivateTenant(id)));
    }

    @DeleteMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateTenant(
            @PathVariable UUID id,
            HttpServletRequest request) {

        tenantService.deactivateTenant(id);
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Tenant deactivated", request.getRequestURI(), null));
    }

    @PostMapping("/{id}/reset-admin")
    public ResponseEntity<ApiResponse<Void>> resetAdminCredentials(
            @PathVariable UUID id,
            HttpServletRequest request) {

        tenantService.resetBankAdminCredentials(id);
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Admin credentials reset and sent via email", request.getRequestURI(), null));
    }


    @GetMapping("/validate/tenant-code")
    public ResponseEntity<ApiResponse<Boolean>> checkTenantCodeAvailability(
            @RequestParam String code,
            HttpServletRequest request) {

        boolean isAvailable = tenantService.isTenantCodeAvailable(code);
        String message = isAvailable ? "Tenant code is available" : "Tenant code is already taken";

        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, message, request.getRequestURI(), isAvailable));
    }

    @GetMapping("/validate/schema-name")
    public ResponseEntity<ApiResponse<Boolean>> checkSchemaNameAvailability(
            @RequestParam String name,
            HttpServletRequest request) {

        boolean isAvailable = tenantService.isSchemaNameAvailable(name);
        String message = isAvailable ? "Schema name is available" : "Schema name is already taken";

        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, message, request.getRequestURI(), isAvailable));
    }
}