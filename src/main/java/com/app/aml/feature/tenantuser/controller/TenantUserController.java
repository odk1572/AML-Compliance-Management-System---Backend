package com.app.aml.feature.tenantuser.controller;

import com.app.aml.domain.api.ApiResponse;
import com.app.aml.feature.tenantuser.dto.ChangePasswordRequestDto;
import com.app.aml.feature.tenantuser.dto.CreateTenantUserRequestDto;
import com.app.aml.feature.tenantuser.dto.TenantUserResponseDto;
import com.app.aml.feature.tenantuser.dto.UpdateTenantUserRequestDto;
import com.app.aml.feature.tenantuser.service.TenantUserService;
import com.app.aml.multitenency.TenantContext;
import com.app.aml.security.rbac.Role;
import com.app.aml.security.userDetails.TenantUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class TenantUserController {

    private final TenantUserService tenantUserService;

    /**
     * Create a new compliance officer (Bank User).
     */

    @PatchMapping("/{id}/reactivate")
    @PreAuthorize("hasAnyRole('BANK_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> reactivateUser(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantIdHeader,
            HttpServletRequest request) {

        // If a Super Admin is calling this, they must provide the tenant ID in the header
        if (tenantIdHeader != null) {
            TenantContext.setTenantId(tenantIdHeader);
        }

        try {
            tenantUserService.reactivateUser(id);
            return ResponseEntity.ok(ApiResponse.of(
                    HttpStatus.OK,
                    "User account reactivated successfully",
                    request.getRequestURI(),
                    null
            ));
        } finally {
            // Clear if it was manually set via header
            if (tenantIdHeader != null) {
                TenantContext.clear();
            }
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('BANK_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TenantUserResponseDto>> createComplianceOfficer(
            @Valid @RequestBody CreateTenantUserRequestDto requestDto,
            HttpServletRequest request) {

        TenantUserResponseDto data = tenantUserService.createComplianceOfficer(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(
                        HttpStatus.CREATED,
                        "User created successfully and onboarding email sent",
                        request.getRequestURI(),
                        data
                ));
    }

    /**
     * Update user profile, role, and lock status.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('BANK_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TenantUserResponseDto>> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTenantUserRequestDto dto,
            HttpServletRequest request) {

        TenantUserResponseDto data = tenantUserService.updateUser(id, dto);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "User updated successfully",
                request.getRequestURI(),
                data
        ));
    }

    /**
     * Get details of a specific user.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('BANK_ADMIN', 'SUPER_ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<TenantUserResponseDto>> getUserById(
            @PathVariable UUID id,
            HttpServletRequest request) {

        TenantUserResponseDto data = tenantUserService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "User details retrieved successfully",
                request.getRequestURI(),
                data
        ));
    }

    /**
     * List all users with optional role filtering.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('BANK_ADMIN', 'SUPER_ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<Page<TenantUserResponseDto>>> listUsers(
            @RequestParam(required = false) Role role,
            @PageableDefault(size = 20) Pageable pageable,
            HttpServletRequest request) {

        Page<TenantUserResponseDto> data = tenantUserService.listUsers(role, pageable);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Users list retrieved successfully",
                request.getRequestURI(),
                data
        ));
    }

    /**
     * Soft delete/deactivate a user and unassign their cases.
     */
    @DeleteMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('BANK_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(
            @PathVariable UUID id,
            HttpServletRequest request) {

        tenantUserService.deactivateUser(id);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "User deactivated successfully and cases unassigned",
                request.getRequestURI(),
                null
        ));
    }

    /**
     * Explicitly unlock a user account (Alternative to PutMapping).
     */
    @PatchMapping("/{id}/unlock")
    @PreAuthorize("hasAnyRole('BANK_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unlockUser(
            @PathVariable UUID id,
            HttpServletRequest request) {

        tenantUserService.unlockUser(id);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "User account unlocked successfully",
                request.getRequestURI(),
                null
        ));
    }

    /**
     * Administrative password reset (generates temporary password).
     */
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAnyRole('BANK_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable UUID id,
            HttpServletRequest request) {

        tenantUserService.resetPassword(id);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Password reset successfully and new temporary password emailed",
                request.getRequestURI(),
                null
        ));
    }

    /**
     * Self-service password change (required after first login).
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal TenantUserDetails currentUser,
            @Valid @RequestBody ChangePasswordRequestDto dto,
            HttpServletRequest request) {

        tenantUserService.changePassword(currentUser.getTenantUser().getId(), dto.getOldPassword(), dto.getNewPassword());
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Password changed successfully",
                request.getRequestURI(),
                null
        ));
    }
}