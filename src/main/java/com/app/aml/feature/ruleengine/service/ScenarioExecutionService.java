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

        for (ConditionExecutionContextDto cond : executionContext.getConditions()) {
            log.info("  - attr={}, agg={}, op={}, threshold={}, lookback={}",
                    cond.getAttributeName(), cond.getAggregationFunction(),
                    cond.getOperator(), cond.getThresholdValue(), cond.getLookbackPeriod());
        }

        // Fetch the correct strategy
        RuleExecutorStrategy strategy = ruleExecutorFactory.getStrategy(categoryName);

        // Run the SQL (Returns Entity-Centric Set<UUID> for Customer IDs)
        Set<UUID> breachingCustomers = strategy.executeRule(executionContext);

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

            conditions.add(ConditionExecutionContextDto.builder()
                    .attributeName(global.getAttributeName())
                    .operator(global.getOperator())
                    .aggregationFunction(resolveOverride(
                            override != null ? override.getOverrideAggregationFunction() : null,
                            global.getAggregationFunction()))
                    .thresholdValue(resolveOverride(
                            override != null ? override.getOverrideValue() : null,
                            global.getThresholdValue()))
                    .lookbackPeriod(resolveOverride(
                            override != null ? override.getOverrideLookbackPeriod() : null,
                            global.getLookbackPeriod()))
                    .build());
        }

        return RuleExecutionContextDto.builder()
                .scenarioId(scenarioId)
                .tenantRuleId(tenantRuleId)
                .ruleId(ruleInfo.getId())
                .ruleName(ruleInfo.getRuleName())
                .baseRiskScore(ruleInfo.getBaseRiskScore())
                .conditionLogic(ruleInfo.getConditionLogic())
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