package com.app.aml.feature.ruleengine.controller;

import com.app.aml.feature.ruleengine.service.ScenarioOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/rule-engine/scenarios")
@RequiredArgsConstructor
public class ScenarioExecutionController {

    private final ScenarioOrchestrationService orchestrationService;

    /**
     * Executes all active rules mapped to a specific Tenant Scenario.
     * * @param tenantScenarioId The UUID of the TenantScenario to execute
     * @return A Set of Customer UUIDs that breached the scenario thresholds
     */
    @PostMapping("/{tenantScenarioId}/execute")
    public ResponseEntity<Set<UUID>> executeScenario(@PathVariable UUID tenantScenarioId) {
        log.info("=========================================================================");
        log.info("RECEIVED API REQUEST: Execute Tenant Scenario [{}]", tenantScenarioId);
        log.info("=========================================================================");

        long startTime = System.currentTimeMillis();

        try {
            // Trigger the orchestration service
            Set<UUID> breachingCustomers = orchestrationService.executeFullScenario(tenantScenarioId);

            long duration = System.currentTimeMillis() - startTime;
            log.info("=========================================================================");
            log.info("SCENARIO EXECUTION COMPLETE [{}]", tenantScenarioId);
            log.info("Result: {} Breaching Customers Found", breachingCustomers.size());
            log.info("Execution Time: {} ms", duration);
            log.info("=========================================================================");

            // Note: If you want to use your custom ApiResponse class, you can wrap the Set
            // e.g., return ResponseEntity.ok(ApiResponse.success(breachingCustomers));
            return ResponseEntity.ok(breachingCustomers);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("VALIDATION ERROR during scenario execution [{}]: {}", tenantScenarioId, e.getMessage());
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            log.error("SYSTEM ERROR during scenario execution [{}]", tenantScenarioId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}