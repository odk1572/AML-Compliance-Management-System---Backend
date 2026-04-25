package com.app.aml.feature.ruleengine.service;

import com.app.aml.feature.ruleengine.dto.globalRuleCondition.response.GlobalRuleConditionResponseDto;
import com.app.aml.feature.ruleengine.dto.globalRules.response.GlobalRuleResponseDto;
import com.app.aml.feature.ruleengine.dto.tenantRuleThreshold.response.TenantRuleThresholdResponseDto;
import com.app.aml.feature.ruleengine.entity.GlobalRule;
import com.app.aml.feature.ruleengine.entity.GlobalRuleCondition;
import com.app.aml.feature.ruleengine.entity.GlobalScenario;
import com.app.aml.feature.ruleengine.entity.TenantRule;
import com.app.aml.feature.ruleengine.entity.TenantRuleThreshold;
import com.app.aml.feature.ruleengine.entity.TenantScenario;
import com.app.aml.feature.ruleengine.mapper.GlobalRuleConditionMapper;
import com.app.aml.feature.ruleengine.mapper.GlobalRuleMapper;
import com.app.aml.feature.ruleengine.mapper.TenantRuleThresholdMapper;
import com.app.aml.feature.ruleengine.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioOrchestrationService {

    private final TenantScenarioRepository tenantScenarioRepository;
    private final TenantRuleRepository tenantRuleRepository;
    private final TenantRuleThresholdRepository tenantRuleThresholdRepository;
    private final GlobalRuleRepository globalRuleRepository;
    private final GlobalRuleConditionRepository globalRuleConditionRepository;
    private final GlobalScenarioRepository globalScenarioRepository;

    private final GlobalRuleMapper globalRuleMapper;
    private final GlobalRuleConditionMapper globalRuleConditionMapper;
    private final TenantRuleThresholdMapper tenantRuleThresholdMapper;

    private final ScenarioExecutionService scenarioExecutionService;

    @Transactional(readOnly = true)
    public Set<UUID> executeFullScenario(UUID tenantScenarioId) {

        // 1. Load TenantScenario (Contains AND/OR logic)
        TenantScenario tenantScenario = tenantScenarioRepository.findById(tenantScenarioId)
                .orElseThrow(() -> new IllegalArgumentException("TenantScenario not found: " + tenantScenarioId));

        String conditionLogic = tenantScenario.getConditionLogic();

        // 2. OPTIMIZATION: Load GlobalScenario ONCE to get the Category for all rules
        GlobalScenario globalScenario = globalScenarioRepository.findById(tenantScenario.getGlobalScenarioId())
                .orElseThrow(() -> new IllegalArgumentException("GlobalScenario not found: " + tenantScenario.getGlobalScenarioId()));

        String categoryName = globalScenario.getCategory();

        log.info("Orchestrating scenario {} (Category: {}) with conditionLogic={}",
                tenantScenarioId, categoryName, conditionLogic);

        // 3. Load all active tenant rules under this scenario
        List<TenantRule> tenantRules = tenantRuleRepository.findByTenantScenarioIdAndIsActiveTrue(tenantScenarioId);

        if (tenantRules.isEmpty()) {
            log.warn("No active rules found for TenantScenario: {}", tenantScenarioId);
            return Collections.emptySet();
        }

        log.info("Found {} active rules for scenario {}", tenantRules.size(), tenantScenarioId);

        // 4. Execute each rule and collect/merge results
        Set<UUID> mergedResult = null;
        boolean isFirstRule = true;

        for (TenantRule tenantRule : tenantRules) {
            // Pass the categoryName down so we don't query the DB again
            Set<UUID> ruleResult = executeSingleRule(tenantScenarioId, tenantRule, categoryName);

            if (isFirstRule) {
                // Initialize with the first rule's result
                mergedResult = new HashSet<>(ruleResult);
                isFirstRule = false;
            } else {
                // Merge using the bank admin's conditionLogic
                if ("AND".equalsIgnoreCase(conditionLogic)) {
                    mergedResult.retainAll(ruleResult);

                    // Fast-fail optimization: If Intersection is ever empty, we can stop evaluating rules early
                    if (mergedResult.isEmpty()) break;

                } else if ("OR".equalsIgnoreCase(conditionLogic)) {
                    mergedResult.addAll(ruleResult);
                } else {
                    throw new IllegalStateException(
                            "Invalid conditionLogic '" + conditionLogic + "' on TenantScenario " + tenantScenarioId + ". Must be 'AND' or 'OR'.");
                }
            }

            log.info("After merging rule '{}' (logic={}): {} cumulative breaching customers",
                    tenantRule.getRuleName(), conditionLogic, mergedResult.size());
        }

        Set<UUID> finalResult = mergedResult != null ? mergedResult : Collections.emptySet();
        log.info("Scenario {} complete. Total breaching customers: {}", tenantScenarioId, finalResult.size());

        return finalResult;
    }

    private Set<UUID> executeSingleRule(UUID tenantScenarioId, TenantRule tenantRule, String categoryName) {

        // 1. Load the GlobalRule definition
        GlobalRule globalRule = globalRuleRepository.findById(tenantRule.getGlobalRuleId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "GlobalRule not found: " + tenantRule.getGlobalRuleId()
                                + " (referenced by TenantRule " + tenantRule.getId() + ")"));
        GlobalRuleResponseDto ruleInfo = globalRuleMapper.toResponseDto(globalRule);

        // 2. Load global conditions
        // (Note: Removed 'OrderByConditionSequenceAsc' since the new executors map purely by attributeName)
        List<GlobalRuleCondition> globalConditions = globalRuleConditionRepository.findByRuleId(globalRule.getId());
        List<GlobalRuleConditionResponseDto> globalConditionDtos = globalRuleConditionMapper.toResponseDtoList(globalConditions);

        // 3. Load tenant-specific threshold overrides
        List<TenantRuleThreshold> tenantOverrides = tenantRuleThresholdRepository.findByTenantRuleId(tenantRule.getId());
        List<TenantRuleThresholdResponseDto> overrideDtos = tenantRuleThresholdMapper.toResponseDtoList(tenantOverrides);

        log.info("Executing TenantRule '{}' (id={}, globalRuleId={}, category={})",
                tenantRule.getRuleName(), tenantRule.getId(), globalRule.getId(), categoryName);

        // 4. Delegate to ScenarioExecutionService
        return scenarioExecutionService.executeScenario(
                tenantScenarioId,
                tenantRule.getId(),
                categoryName,
                ruleInfo,
                globalConditionDtos,
                overrideDtos
        );
    }
}