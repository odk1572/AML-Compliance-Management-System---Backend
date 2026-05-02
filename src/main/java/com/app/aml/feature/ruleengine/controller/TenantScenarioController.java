package com.app.aml.feature.ruleengine.controller;


import com.app.aml.apiResponse.ApiResponse;
import com.app.aml.feature.ruleengine.dto.tenantRule.response.TenantRuleResponseDto;
import com.app.aml.feature.ruleengine.dto.tenantScenario.response.TenantScenarioResponseDto;
import com.app.aml.feature.ruleengine.dto.tenantScenario.response.TenantScenarioWithRulesDto;
import com.app.aml.feature.ruleengine.service.TenantScenarioService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Add these imports at the top:
import com.app.aml.feature.ruleengine.dto.globalScenario.response.GlobalScenarioResponseDto;
import com.app.aml.feature.ruleengine.service.GlobalScenarioService;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/tenant-scenarios")
@RequiredArgsConstructor
public class TenantScenarioController {

    private final TenantScenarioService tenantScenarioService;

    @PostMapping("/activate/{globalScenarioId}")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<ApiResponse<TenantScenarioResponseDto>> activateScenario(
            @PathVariable UUID globalScenarioId,
            HttpServletRequest request) {

        log.info("REST request to activate Global Scenario: {}", globalScenarioId);
        TenantScenarioResponseDto response = tenantScenarioService.activateScenario(globalScenarioId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(
                        HttpStatus.CREATED,
                        "Scenario activated successfully",
                        request.getRequestURI(),
                        response
                ));
    }

    @PutMapping("/{id}/pause")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<ApiResponse<TenantScenarioResponseDto>> pauseScenario(
            @PathVariable("id") UUID tenantScenarioId,
            HttpServletRequest request) {

        log.info("REST request to pause Tenant Scenario: {}", tenantScenarioId);
        TenantScenarioResponseDto response = tenantScenarioService.pauseScenario(tenantScenarioId);

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Scenario paused successfully",
                request.getRequestURI(),
                response
        ));
    }

    @PutMapping("/{id}/resume")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<ApiResponse<TenantScenarioResponseDto>> resumeScenario(
            @PathVariable("id") UUID tenantScenarioId,
            HttpServletRequest request) {

        log.info("REST request to resume Tenant Scenario: {}", tenantScenarioId);
        TenantScenarioResponseDto response = tenantScenarioService.resumeScenario(tenantScenarioId);

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Scenario resumed successfully",
                request.getRequestURI(),
                response
        ));
    }

    @PatchMapping("/rules/{ruleId}/toggle")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<ApiResponse<TenantRuleResponseDto>> toggleScenarioRule(
            @PathVariable("ruleId") UUID tenantRuleId,
            @RequestParam(name = "active") boolean isActive,
            HttpServletRequest request) {

        log.info("REST request to toggle Tenant Rule: {} to active: {}", tenantRuleId, isActive);
        TenantRuleResponseDto response = tenantScenarioService.toggleScenarioRule(tenantRuleId, isActive);

        String statusMessage = isActive ? "Rule enabled successfully" : "Rule disabled successfully";

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                statusMessage,
                request.getRequestURI(),
                response
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<ApiResponse<TenantScenarioWithRulesDto>> getScenarioByIdWithRules(
            @PathVariable("id") UUID tenantScenarioId,
            HttpServletRequest request) {

        log.info("REST request to fetch Tenant Scenario details: {}", tenantScenarioId);
        TenantScenarioWithRulesDto response = tenantScenarioService.getScenarioByIdWithRules(tenantScenarioId);

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Scenario details fetched successfully",
                request.getRequestURI(),
                response
        ));
    }

    @GetMapping
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<ApiResponse<List<TenantScenarioWithRulesDto>>> listActiveScenariosWithRules(
            HttpServletRequest request) {

        log.info("REST request to list all active Tenant Scenarios with rules");
        List<TenantScenarioWithRulesDto> response = tenantScenarioService.listActiveScenariosWithRules();

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Active scenarios fetched successfully",
                request.getRequestURI(),
                response
        ));
    }

    @GetMapping("/available-globals")
    @PreAuthorize("hasAnyRole('BANK_ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<List<GlobalScenarioResponseDto>>> getAvailableGlobalScenarios(
            HttpServletRequest request) {

        log.info("REST request to fetch available Global Scenarios for tenant activation");
        List<GlobalScenarioResponseDto> response = tenantScenarioService.getAvailableGlobalScenarios();

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Available global scenarios fetched successfully",
                request.getRequestURI(),
                response
        ));
    }

}