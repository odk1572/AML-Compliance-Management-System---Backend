package com.app.aml.feature.ruleengine.controller;

import com.app.aml.apiResponse.ApiResponse;
import com.app.aml.feature.ruleengine.dto.RuleBreachResult;
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
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/rule-engine/scenarios")
@RequiredArgsConstructor
public class ScenarioExecutionController {

    private final ScenarioOrchestrationService orchestrationService;

    @PostMapping("/{tenantScenarioId}/execute")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BANK_ADMIN')")
    public ResponseEntity<ApiResponse<ScenarioExecutionSummary>> executeScenario(
            @PathVariable UUID tenantScenarioId,
            HttpServletRequest request) {

        log.info("=========================================================================");
        log.info("STARTING EXECUTION: Tenant Scenario [{}]", tenantScenarioId);
        log.info("=========================================================================");

        long startTime = System.currentTimeMillis();

        // Updated to receive the full DTO list
        List<RuleBreachResult> ruleBreaches = orchestrationService.executeFullScenario(tenantScenarioId);

        long duration = System.currentTimeMillis() - startTime;

        ScenarioExecutionSummary summary = ScenarioExecutionSummary.builder()
                .tenantScenarioId(tenantScenarioId)
                .executionTimestamp(LocalDateTime.now())
                .totalBreachesFound(ruleBreaches.size())
                .breaches(ruleBreaches) // Passing the full list of Customers and Transactions
                .processingTimeMs(duration)
                .status("SUCCESS")
                .build();

        log.info("=========================================================================");
        log.info("EXECUTION COMPLETE: {} breaches flagged in {} ms", ruleBreaches.size(), duration);
        log.info("=========================================================================");

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Scenario execution completed successfully.",
                request.getRequestURI(),
                summary
        ));
    }

    @Data
    @Builder
    public static class ScenarioExecutionSummary {
        private UUID tenantScenarioId;
        private LocalDateTime executionTimestamp;
        private int totalBreachesFound;
        private List<RuleBreachResult> breaches; // Updated field type
        private long processingTimeMs;
        private String status;
    }
}