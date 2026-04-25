package com.app.aml.feature.ruleengine.service;

import com.app.aml.feature.ingestion.entity.CustomerProfile;
import com.app.aml.feature.ingestion.repository.CustomerProfileRepository;
import com.app.aml.feature.ruleengine.dto.execution.ConditionExecutionContextDto;
import com.app.aml.feature.ruleengine.dto.execution.RuleExecutionContextDto;
import com.app.aml.feature.ruleengine.dto.globalRuleCondition.response.GlobalRuleConditionResponseDto;
import com.app.aml.feature.ruleengine.dto.globalRules.response.GlobalRuleResponseDto;
import com.app.aml.feature.ruleengine.dto.tenantRuleThreshold.response.TenantRuleThresholdResponseDto;
import com.app.aml.feature.ruleengine.executor.RuleExecutorFactory;
import com.app.aml.feature.ruleengine.executor.RuleExecutorStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioExecutionService {

    private final RuleExecutorFactory ruleExecutorFactory;
    private final CustomerProfileRepository customerProfileRepository;

    @Transactional(readOnly = true)
    public Set<UUID> executeScenario(
            UUID scenarioId,
            UUID tenantRuleId,
            String categoryName,
            GlobalRuleResponseDto ruleInfo,
            List<GlobalRuleConditionResponseDto> globals,
            List<TenantRuleThresholdResponseDto> overrides) {

        log.info("Starting execution for Category: {} under Scenario: {}", categoryName, scenarioId);

        // Flatten DB mapping to Execution Context
        RuleExecutionContextDto executionContext = buildExecutionContext(
                scenarioId, tenantRuleId, categoryName, ruleInfo, globals, overrides);

        // Log the newly mapped pure-attribute parameters
        for (ConditionExecutionContextDto cond : executionContext.getConditions()) {
            log.info("  - attribute={}, threshold={}",
                    cond.getAttributeName(),
                    cond.getThresholdValue());
        }

        // Fetch the correct strategy using the specific ruleType
        RuleExecutorStrategy strategy = ruleExecutorFactory.getStrategy(ruleInfo.getRuleType());

        // Print Pre-Execution Header
        log.info("===============================================================");
        log.info("▶ STARTING EXECUTION FOR RULE: {}", ruleInfo.getRuleName());
        log.info("▶ STRATEGY TYPE: {}", ruleInfo.getRuleType());
        log.info("===============================================================");

        // Run the SQL (Returns Entity-Centric Set<UUID> for Customer IDs)
        Set<UUID> breachingCustomers = strategy.executeRule(executionContext);

        // Print Stylized Results
        if (breachingCustomers.isEmpty()) {
            log.info("✅ NO BREACHES FOUND. All customers passed the rule thresholds.");
        } else {
            log.info("🚨 RULE BREACHED! Found {} customers violating the rule thresholds.", breachingCustomers.size());

            int counter = 1;
            for (UUID customerId : breachingCustomers) {
                // Fetch the human-readable account number from the DB
                String accountNumber = customerProfileRepository.findById(customerId)
                        .map(CustomerProfile::getAccountNumber)
                        .orElse("UNKNOWN_ACC");

                log.info("   [!] Offender {}: Customer ID -> {} ({})", counter++, customerId, accountNumber);
            }
        }
        log.info("===============================================================\n");

        log.info("Rule '{}' found {} breaching customers under Scenario: {}",
                ruleInfo.getRuleName(), breachingCustomers.size(), scenarioId);

        return breachingCustomers;
    }

    private RuleExecutionContextDto buildExecutionContext(
            UUID scenarioId,
            UUID tenantRuleId,
            String category,
            GlobalRuleResponseDto ruleInfo,
            List<GlobalRuleConditionResponseDto> globals,
            List<TenantRuleThresholdResponseDto> overrides) {

        List<ConditionExecutionContextDto> conditions = new ArrayList<>();

        for (GlobalRuleConditionResponseDto global : globals) {
            TenantRuleThresholdResponseDto override = overrides.stream()
                    .filter(o -> o.getGlobalConditionId().equals(global.getId()))
                    .findFirst()
                    .orElse(null);

            // Create the context mapping purely based on the attribute name
            conditions.add(ConditionExecutionContextDto.builder()
                    .attributeName(global.getAttributeName())
                    .thresholdValue(resolveOverride(
                            override != null ? override.getOverrideValue() : null,
                            global.getThresholdValue()))
                    .build());
        }

        return RuleExecutionContextDto.builder()
                .scenarioId(scenarioId)
                .tenantRuleId(tenantRuleId)
                .ruleId(ruleInfo.getId())
                .ruleName(ruleInfo.getRuleName())
                .baseRiskScore(ruleInfo.getBaseRiskScore() != null ? ruleInfo.getBaseRiskScore().intValue() : 0)
                .ruleCategory(category)
                .conditions(conditions)
                .build();
    }

    private String resolveOverride(String overrideValue, String globalValue) {
        if (overrideValue != null && !overrideValue.isBlank()) {
            return overrideValue;
        }
        return globalValue;
    }
}