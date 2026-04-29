package com.app.aml.feature.ruleengine.controller;

import com.app.aml.apiResponse.ApiResponse;
import com.app.aml.feature.ruleengine.dto.tenantRule.request.UpdateTenantRuleRequestDto;
import com.app.aml.feature.ruleengine.dto.tenantRule.response.TenantRuleResponseDto;
import com.app.aml.feature.ruleengine.dto.tenantRuleThreshold.request.CreateTenantRuleThresholdRequestDto;
import com.app.aml.feature.ruleengine.dto.tenantRuleThreshold.request.UpdateTenantRuleThresholdRequestDto;
import com.app.aml.feature.ruleengine.dto.tenantRuleThreshold.response.TenantRuleThresholdResponseDto;
import com.app.aml.feature.ruleengine.service.TenantRuleService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/tenant-rules")
@RequiredArgsConstructor
public class TenantRuleController {

    private final TenantRuleService tenantRuleService;


    @PreAuthorize("hasRole( 'BANK_ADMIN')")
    @GetMapping("/{ruleId}")
    public ResponseEntity<ApiResponse<TenantRuleResponseDto>> getRuleById(
            @PathVariable UUID ruleId,
            HttpServletRequest request) {
        log.info("REST request to get Tenant Rule ID: {}", ruleId);
        TenantRuleResponseDto response = tenantRuleService.getRuleById(ruleId);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Tenant rule fetched successfully",
                request.getRequestURI(),
                response
        ));
    }

    @PreAuthorize("hasRole( 'BANK_ADMIN')")
    @PutMapping("/{ruleId}")
    public ResponseEntity<ApiResponse<TenantRuleResponseDto>> updateRule(
            @PathVariable UUID ruleId,
            @Valid @RequestBody UpdateTenantRuleRequestDto dto,
            HttpServletRequest request) {
        log.info("REST request to update Tenant Rule ID: {}", ruleId);
        TenantRuleResponseDto response = tenantRuleService.updateRule(ruleId, dto);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Tenant rule updated successfully",
                request.getRequestURI(),
                response
        ));
    }

    @PreAuthorize("hasRole('BANK_ADMIN')")
    @PatchMapping("/{ruleId}/toggle")
    public ResponseEntity<ApiResponse<TenantRuleResponseDto>> toggleRule(
            @PathVariable UUID ruleId,
            @RequestParam(name = "active") boolean isActive,
            HttpServletRequest request) {
        log.info("REST request to toggle Tenant Rule ID: {} to active: {}", ruleId, isActive);
        TenantRuleResponseDto response = tenantRuleService.toggleRule(ruleId, isActive);
        String statusMessage = isActive ? "Rule enabled successfully" : "Rule disabled successfully";
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                statusMessage,
                request.getRequestURI(),
                response
        ));
    }

    // ========================================================================
    // Tenant Rule Threshold Override Endpoints
    // ========================================================================

    @PreAuthorize("hasRole('BANK_ADMIN')")
    @GetMapping("/{ruleId}/thresholds")
    public ResponseEntity<ApiResponse<List<TenantRuleThresholdResponseDto>>> getThresholdsForRule(
            @PathVariable UUID ruleId,
            HttpServletRequest request) {
        log.info("REST request to get thresholds for Tenant Rule ID: {}", ruleId);
        List<TenantRuleThresholdResponseDto> response = tenantRuleService.getThresholdsForRule(ruleId);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Threshold overrides fetched successfully",
                request.getRequestURI(),
                response
        ));
    }

    @PreAuthorize("hasRole('BANK_ADMIN')")
    @PostMapping("/thresholds")
    public ResponseEntity<ApiResponse<TenantRuleThresholdResponseDto>> createThresholdOverride(
            @Valid @RequestBody CreateTenantRuleThresholdRequestDto dto,
            HttpServletRequest request) {
        TenantRuleThresholdResponseDto response = tenantRuleService.createThresholdOverride(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(
                HttpStatus.CREATED,
                "Threshold override created successfully",
                request.getRequestURI(),
                response
        ));
    }

    @PreAuthorize("hasRole('BANK_ADMIN')")
    @PutMapping("/thresholds/{thresholdId}")
    public ResponseEntity<ApiResponse<TenantRuleThresholdResponseDto>> updateThresholdOverride(
            @PathVariable UUID thresholdId,
            @Valid @RequestBody UpdateTenantRuleThresholdRequestDto dto,
            HttpServletRequest request) {
        log.info("REST request to update Threshold Override ID: {}", thresholdId);
        TenantRuleThresholdResponseDto response = tenantRuleService.updateThresholdOverride(thresholdId, dto);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Threshold override updated successfully",
                request.getRequestURI(),
                response
        ));
    }

    @PreAuthorize("hasRole('BANK_ADMIN')")
    @DeleteMapping("/thresholds/{thresholdId}")
    public ResponseEntity<ApiResponse<Void>> deleteThresholdOverride(
            @PathVariable UUID thresholdId,
            HttpServletRequest request) {
        log.info("REST request to delete Threshold Override ID: {}", thresholdId);
        tenantRuleService.deleteThresholdOverride(thresholdId);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Threshold override deleted successfully",
                request.getRequestURI(),
                null
        ));
    }

    @PreAuthorize("hasRole('BANK_ADMIN')")
    @PutMapping("/{ruleId}/thresholds/bulk")
    public ResponseEntity<ApiResponse<List<TenantRuleThresholdResponseDto>>> updateThresholds(
            @PathVariable UUID ruleId,
            @Valid @RequestBody List<CreateTenantRuleThresholdRequestDto> overrides,
            HttpServletRequest request) {
        log.info("REST request to bulk update thresholds for Tenant Rule ID: {}", ruleId);
        List<TenantRuleThresholdResponseDto> response = tenantRuleService.updateThresholds(ruleId, overrides);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Threshold overrides bulk updated successfully",
                request.getRequestURI(),
                response
        ));
    }
}