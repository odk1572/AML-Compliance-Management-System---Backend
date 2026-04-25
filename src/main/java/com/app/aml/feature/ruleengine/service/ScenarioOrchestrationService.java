package com.app.aml.feature.ruleengine.service;

import com.app.aml.feature.ruleengine.dto.globalRuleCondition.response.GlobalRuleConditionResponseDto;
import com.app.aml.feature.ruleengine.dto.globalRules.response.GlobalRuleResponseDto;
import com.app.aml.feature.ruleengine.dto.tenantRuleThreshold.response.TenantRuleThresholdResponseDto;
import com.app.aml.feature.ruleengine.entity.GlobalRule;
import com.app.aml.feature.ruleengine.entity.GlobalRuleCondition;
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

        // Load TenantScenario — contains the bank admin's AND/OR choice
        TenantScenario tenantScenario = tenantScenarioRepository.findById(tenantScenarioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "TenantScenario not found: " + tenantScenarioId));

        String conditionLogic = tenantScenario.getConditionLogic();
        log.info("Orchestrating scenario {} with conditionLogic={}", tenantScenarioId, conditionLogic);

        // Load all active tenant rules under this scenario
        List<TenantRule> tenantRules = tenantRuleRepository
                .findByTenantScenarioIdAndIsActiveTrue(tenantScenarioId);

        if (tenantRules.isEmpty()) {
            log.warn("No active rules found for TenantScenario: {}", tenantScenarioId);
            return Collections.emptySet();
        }

        log.info("Found {} active rules for scenario {}", tenantRules.size(), tenantScenarioId);

        // Execute each rule and collect results
        Set<UUID> mergedResult = null;
        boolean isFirstRule = true;

        for (TenantRule tenantRule : tenantRules) {
            Set<UUID> ruleResult = executeSingleRule(tenantScenarioId, tenantRule);

            if (isFirstRule) {
                // Initialize with the first rule's result
                mergedResult = new HashSet<>(ruleResult);
                isFirstRule = false;
            } else {
                // Merge using the bank admin's conditionLogic
                if ("AND".equalsIgnoreCase(conditionLogic)) {
                    mergedResult.retainAll(ruleResult);
                } else if ("OR".equalsIgnoreCase(conditionLogic)) {
                    mergedResult.addAll(ruleResult);
                } else {
                    throw new IllegalStateException(
                            "Invalid conditionLogic '" + conditionLogic + "' on TenantScenario " + tenantScenarioId
                                    + ". Must be 'AND' or 'OR'.");
                }
            }

            log.info("After merging rule '{}' (logic={}): {} cumulative breaching customers",
                    tenantRule.getRuleName(), conditionLogic,
                    mergedResult != null ? mergedResult.size() : 0);
        }

        Set<UUID> finalResult = mergedResult != null ? mergedResult : Collections.emptySet();
        log.info("Scenario {} complete. Total breaching customers: {}", tenantScenarioId, finalResult.size());

        return finalResult;
    }


//    private Set<UUID> executeSingleRule(UUID tenantScenarioId, TenantRule tenantRule) {
//
//        // Load the GlobalRule definition
//        GlobalRule globalRule = globalRuleRepository.findById(tenantRule.getGlobalRuleId())
//                .orElseThrow(() -> new IllegalArgumentException(
//                        "GlobalRule not found: " + tenantRule.getGlobalRuleId()
//                                + " (referenced by TenantRule " + tenantRule.getId() + ")"));
//        GlobalRuleResponseDto ruleInfo = globalRuleMapper.toResponseDto(globalRule);
//
//        // Load global conditions (ordered by sequence)
//        List<GlobalRuleCondition> globalConditions = globalRuleConditionRepository
//                .findByRuleIdOrderByConditionSequenceAsc(globalRule.getId());
//        List<GlobalRuleConditionResponseDto> globalConditionDtos = globalRuleConditionMapper
//                .toResponseDtoList(globalConditions);
//
//        // Load tenant-specific threshold overrides
//        List<TenantRuleThreshold> tenantOverrides = tenantRuleThresholdRepository
//                .findByTenantRuleId(tenantRule.getId());
//        List<TenantRuleThresholdResponseDto> overrideDtos = tenantRuleThresholdMapper
//                .toResponseDtoList(tenantOverrides);
//
//        // Determine the rule category (used for strategy dispatch)
//        String categoryName = globalRule.getRuleName().toUpperCase().replace(" ", "_");
//
//        log.info("Executing TenantRule '{}' (id={}, globalRuleId={}, category={})",
//                tenantRule.getRuleName(), tenantRule.getId(), globalRule.getId(), categoryName);
//
//        // Delegate to ScenarioExecutionService for the actual execution
//        return scenarioExecutionService.executeScenario(
//                tenantScenarioId,
//                tenantRule.getId(),
//                categoryName,
//                ruleInfo,
//                globalConditionDtos,
//                overrideDtos
//        );
//    }

    // ... inside ScenarioOrchestrationService.java

    private Set<UUID> executeSingleRule(UUID tenantScenarioId, TenantRule tenantRule) {

        // 1. Fetch the associated TenantScenario to get the GlobalScenario ID
        TenantScenario tenantScenario = tenantScenarioRepository.findById(tenantScenarioId)
                .orElseThrow(() -> new IllegalArgumentException("TenantScenario not found"));

        // 2. Fetch the GlobalScenario to get the actual TypologyCategory
        com.app.aml.feature.ruleengine.entity.GlobalScenario globalScenario =
                globalScenarioRepository.findById(tenantScenario.getGlobalScenarioId()) // Note: You'll need to inject GlobalScenarioRepository
                        .orElseThrow(() -> new IllegalArgumentException("GlobalScenario not found"));

        // 3. Load the GlobalRule definition
        GlobalRule globalRule = globalRuleRepository.findById(tenantRule.getGlobalRuleId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "GlobalRule not found: " + tenantRule.getGlobalRuleId()
                                + " (referenced by TenantRule " + tenantRule.getId() + ")"));
        GlobalRuleResponseDto ruleInfo = globalRuleMapper.toResponseDto(globalRule);

        // ... (rest of the data fetching logic remains the same)
        List<GlobalRuleCondition> globalConditions = globalRuleConditionRepository
                .findByRuleId(globalRule.getId());
        List<GlobalRuleConditionResponseDto> globalConditionDtos = globalRuleConditionMapper
                .toResponseDtoList(globalConditions);

        List<TenantRuleThreshold> tenantOverrides = tenantRuleThresholdRepository
                .findByTenantRuleId(tenantRule.getId());
        List<TenantRuleThresholdResponseDto> overrideDtos = tenantRuleThresholdMapper
                .toResponseDtoList(tenantOverrides);

        // 4. Use the category from the GlobalScenario instead of deriving from the rule name
        String categoryName = globalScenario.getCategory();

        log.info("Executing TenantRule '{}' (id={}, globalRuleId={}, category={})",
                tenantRule.getRuleName(), tenantRule.getId(), globalRule.getId(), categoryName);

        // Delegate to ScenarioExecutionService
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
