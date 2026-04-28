package com.app.aml.feature.ruleengine.controller;

import com.app.aml.apiResponse.ApiResponse;
import com.app.aml.feature.ruleengine.dto.RuleBreachResult;
import com.app.aml.feature.ruleengine.dto.ScenarioExecutionRequestDto;
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

import java.time.Instant;
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
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<ApiResponse<ScenarioExecutionSummary>> executeScenario(
            @PathVariable UUID tenantScenarioId,
            @RequestBody(required = false) ScenarioExecutionRequestDto requestDto,
            HttpServletRequest request) {

        boolean isForensic = requestDto != null &&
                requestDto.getGlobalLookbackStart() != null &&
                requestDto.getGlobalLookbackEnd() != null;

        String mode = isForensic ? "FORENSIC" : "LIVE";

        log.info("=========================================================================");
        log.info("STARTING {} EXECUTION: Tenant Scenario [{}]", mode, tenantScenarioId);
        if (isForensic) {
            log.info("Time Window: {} to {}", requestDto.getGlobalLookbackStart(), requestDto.getGlobalLookbackEnd());
        }
        log.info("=========================================================================");

        long startTime = System.currentTimeMillis();

        List<RuleBreachResult> ruleBreaches = orchestrationService.executeFullScenario(tenantScenarioId, requestDto);

        long duration = System.currentTimeMillis() - startTime;

        ScenarioExecutionSummary summary = ScenarioExecutionSummary.builder()
                .tenantScenarioId(tenantScenarioId)
                .executionTimestamp(LocalDateTime.now())
                .executionMode(mode)
                .timeWindowStart(requestDto != null ? requestDto.getGlobalLookbackStart() : null)
                .timeWindowEnd(requestDto != null ? requestDto.getGlobalLookbackEnd() : null)
                .totalBreachesFound(ruleBreaches.size())
                .breaches(ruleBreaches)
                .processingTimeMs(duration)
                .status("SUCCESS")
                .build();

        log.info("=========================================================================");
        log.info("EXECUTION COMPLETE: {} breaches flagged in {} ms", ruleBreaches.size(), duration);
        log.info("=========================================================================");

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Scenario execution completed successfully in " + mode + " mode.",
                request.getRequestURI(),
                summary
        ));
    }

    @Data
    @Builder
    public static class ScenarioExecutionSummary {
        private UUID tenantScenarioId;
        private LocalDateTime executionTimestamp;
        private String executionMode;
        private Instant timeWindowStart;
        private Instant timeWindowEnd;
        private int totalBreachesFound;
        private List<RuleBreachResult> breaches;
        private long processingTimeMs;
        private String status;
    }
}