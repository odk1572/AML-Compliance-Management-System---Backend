package com.app.aml.feature.ruleengine.controller;

import com.app.aml.domain.api.ApiResponse;
import com.app.aml.feature.ruleengine.service.ScenarioOrchestrationService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/rule-engine/scenarios")
@RequiredArgsConstructor
public class ScenarioExecutionController {

    private final ScenarioOrchestrationService orchestrationService;

    /**
     * Executes all active rules for a Tenant Scenario.
     * Accessible by SUPER_ADMIN and BANK_ADMIN.
     */
    @PostMapping("/{tenantScenarioId}/execute")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BANK_ADMIN')")
    public ResponseEntity<ApiResponse<ScenarioExecutionSummary>> executeScenario(
            @PathVariable UUID tenantScenarioId,
            HttpServletRequest request) {

        log.info("=========================================================================");
        log.info("STARTING EXECUTION: Tenant Scenario [{}]", tenantScenarioId);
        log.info("=========================================================================");

        long startTime = System.currentTimeMillis();

        // 1. Core Logic Call
        Set<UUID> breachingCustomers = orchestrationService.executeFullScenario(tenantScenarioId);

        long duration = System.currentTimeMillis() - startTime;

        // 2. Prepare Data DTO
        ScenarioExecutionSummary summary = ScenarioExecutionSummary.builder()
                .tenantScenarioId(tenantScenarioId)
                .executionTimestamp(LocalDateTime.now())
                .totalBreachesFound(breachingCustomers.size())
                .breachingCustomerIds(breachingCustomers)
                .processingTimeMs(duration)
                .status("SUCCESS")
                .build();

        log.info("=========================================================================");
        log.info("EXECUTION COMPLETE: {} customers flagged in {} ms", breachingCustomers.size(), duration);
        log.info("=========================================================================");

        // 3. Build Response using ApiResponse.of(...)
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Scenario execution completed successfully.",
                request.getRequestURI(),
                summary
        ));
    }

    // --- Summary DTO ---
    @Data
    @Builder
    public static class ScenarioExecutionSummary {
        private UUID tenantScenarioId;
        private LocalDateTime executionTimestamp;
        private int totalBreachesFound;
        private Set<UUID> breachingCustomerIds;
        private long processingTimeMs;
        private String status;
    }
}