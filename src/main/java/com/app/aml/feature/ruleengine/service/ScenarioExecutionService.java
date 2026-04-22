package com.app.aml.feature.ruleengine.service;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioExecutionService {

    private final RuleExecutorFactory ruleExecutorFactory;

    public void executeScenario(
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

        // Fetch the correct strategy
        RuleExecutorStrategy strategy = ruleExecutorFactory.getStrategy(categoryName);

        // Run the SQL (Now properly returns Entity-Centric Set<UUID> for Customer IDs)
        Set<UUID> breachingCustomers = strategy.executeRule(executionContext);

        // Generate Alerts
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

            conditions.add(ConditionExecutionContextDto.builder()
                    .attributeName(global.getAttributeName())
                    .operator(global.getOperator())
                    .aggregationFunction(override != null && override.getOverrideAggregationFunction() != null
                            ? override.getOverrideAggregationFunction() : global.getAggregationFunction())
                    .thresholdValue(override != null && override.getOverrideValue() != null
                            ? override.getOverrideValue() : global.getThresholdValue())
                    .lookbackPeriod(override != null && override.getOverrideLookbackPeriod() != null
                            ? override.getOverrideLookbackPeriod() : global.getLookbackPeriod())
                    .build());
        }

        return RuleExecutionContextDto.builder()
                .scenarioId(scenarioId)     // Pass to Execution Context
                .tenantRuleId(tenantRuleId) // Pass to Execution Context
                .ruleId(ruleInfo.getId())
                .ruleName(ruleInfo.getRuleName())
                .baseRiskScore(ruleInfo.getBaseRiskScore())
                .ruleCategory(category)
                .conditions(conditions)
                .build();
    }
}