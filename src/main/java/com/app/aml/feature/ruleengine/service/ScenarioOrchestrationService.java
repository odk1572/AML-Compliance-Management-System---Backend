package com.app.aml.feature.ruleengine.service;

import com.app.aml.enums.AlertSeverity;
import com.app.aml.feature.alert.entity.Alert;
import com.app.aml.feature.alert.entity.AlertEvidence;
import com.app.aml.feature.alert.entity.AlertTransaction;
import com.app.aml.feature.alert.repository.AlertEvidenceRepository;
import com.app.aml.feature.alert.repository.AlertRepository;
import com.app.aml.feature.alert.repository.AlertTransactionRepository;
import com.app.aml.feature.ingestion.entity.Transaction;
import com.app.aml.feature.ingestion.repository.TransactionRepository;
import com.app.aml.feature.ruleengine.dto.RuleBreachResult;
import com.app.aml.feature.ruleengine.dto.execution.ConditionExecutionContextDto;

import com.app.aml.feature.ruleengine.dto.execution.RuleExecutionContextDto;
import com.app.aml.feature.ruleengine.dto.tenantRule.response.TenantRuleResponseDto;
import com.app.aml.feature.ruleengine.entity.*;
import com.app.aml.feature.ruleengine.executor.GeographicRiskEvaluator;
import com.app.aml.feature.ruleengine.executor.RuleExecutorFactory;
import com.app.aml.feature.ruleengine.executor.RuleExecutorStrategy;
import com.app.aml.feature.ruleengine.mapper.TenantRuleMapper;
import com.app.aml.feature.ruleengine.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioOrchestrationService {

    private final TenantScenarioRepository tenantScenarioRepo;
    private final TenantRuleRepository tenantRuleRepo;
    private final GlobalRuleRepository globalRuleRepo;
    private final GlobalRuleConditionRepository conditionRepo;
    private final TenantRuleThresholdRepository thresholdRepo;
    private final RuleExecutorFactory executorFactory;
    private final AlertRepository alertRepo;
    private final AlertEvidenceRepository evidenceRepo;
    private final TransactionRepository txnRepo;
    private final AlertTransactionRepository alertTxnRepo;
    private final GeographicRiskEvaluator geoRiskEvaluator;
    private final TenantRuleMapper tenantRuleMapper;

    @Transactional
    public List<RuleBreachResult> executeFullScenario(UUID tenantScenarioId) {
        log.info("Starting execution for Tenant Scenario ID: {}", tenantScenarioId);

        // 1. Change to a List that holds your DTO
        List<RuleBreachResult> allScenarioBreaches = new ArrayList<>();

        TenantScenario tenantScenario = tenantScenarioRepo.findById(tenantScenarioId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant Scenario not found: " + tenantScenarioId));

        List<TenantRuleResponseDto> activeRules = loadActiveRules(tenantScenarioId);
        log.debug("Found {} active rules for scenario.", activeRules.size());

        for (TenantRuleResponseDto tenantRule : activeRules) {
            log.debug("Executing Rule: {} (Global Rule ID: {})", tenantRule.getRuleName(), tenantRule.getGlobalRuleId());

            GlobalRule globalRule = globalRuleRepo.findById(tenantRule.getGlobalRuleId())
                    .orElseThrow(() -> new IllegalArgumentException("Global Rule not found: " + tenantRule.getGlobalRuleId()));

            List<GlobalRuleCondition> conditions = conditionRepo.findByRuleId(globalRule.getId());
            List<TenantRuleThreshold> thresholds = thresholdRepo.findByTenantRuleId(tenantRule.getId());

            RuleExecutionContextDto context = buildExecutionContext(tenantRule, globalRule, conditions, thresholds);
            RuleExecutorStrategy strategy = executorFactory.getStrategy(context.getRuleType());

            log.debug("Handing off to Strategy: {}", strategy.getRuleType());

            // Strategy now returns the full list of Customers and Transactions
            List<RuleBreachResult> breachingResults = strategy.executeRule(context);

            if (!breachingResults.isEmpty()) {
                log.info("Rule [{}] triggered for {} customers.", tenantRule.getRuleName(), breachingResults.size());

                // Persist alerts and evidences
                persistResults(breachingResults, context, tenantScenario);

                // 2. Add the full results to our master list
                allScenarioBreaches.addAll(breachingResults);
            }
        }

        log.info("Scenario execution complete. Total breaches flagged: {}", allScenarioBreaches.size());

        // 3. Return the full list to the Controller
        return allScenarioBreaches;
    }

    private List<TenantRuleResponseDto> loadActiveRules(UUID tenantScenarioId) {
        List<TenantRule> rules = tenantRuleRepo.findByTenantScenarioIdAndIsActiveTrue(tenantScenarioId);
        return tenantRuleMapper.toResponseDtoList(rules);
    }

    private RuleExecutionContextDto buildExecutionContext(TenantRuleResponseDto tenantRule, GlobalRule globalRule,
                                                          List<GlobalRuleCondition> conditions,
                                                          List<TenantRuleThreshold> thresholds) {
        Map<UUID, TenantRuleThreshold> overrideMap = thresholds.stream()
                .collect(Collectors.toMap(TenantRuleThreshold::getGlobalConditionId, t -> t));

        List<ConditionExecutionContextDto> conditionDtos = conditions.stream()
                .map(cond -> mergeThreshold(cond, overrideMap))
                .toList();

        return RuleExecutionContextDto.builder()
                .tenantRuleId(tenantRule.getId())
                .globalRuleId(globalRule.getId())
                .ruleType(globalRule.getRuleType())
                .severity(globalRule.getSeverity())
                .baseRiskScore(globalRule.getBaseRiskScore())
                .typologyLabel(tenantRule.getRuleName())
                .conditions(conditionDtos)
                .build();
    }

    private ConditionExecutionContextDto mergeThreshold(GlobalRuleCondition cond, Map<UUID, TenantRuleThreshold> overrides) {
        TenantRuleThreshold override = overrides.get(cond.getId());

        String aggFn = resolveSafe(
                override != null ? override.getOverrideAggregationFunction() : null,
                cond.getAggregationFunction()
        );

        String lookback = resolveSafe(
                override != null ? override.getOverrideLookbackPeriod() : null,
                cond.getLookbackPeriod()
        );

        String threshold = resolveSafe(
                override != null ? override.getOverrideValue() : null,
                cond.getThresholdValue()
        );

        return ConditionExecutionContextDto.builder()
                .attributeName(cond.getAttributeName())
                .aggregationFunction(aggFn)
                .lookbackPeriod(lookback)
                .thresholdValue(threshold)
                .operator("BREACHED")
                .build();
    }

    private String resolveSafe(String overrideVal, String globalVal) {
        if (overrideVal != null && !overrideVal.isBlank()) {
            return overrideVal.trim();
        }
        if (globalVal != null && !globalVal.isBlank()) {
            return globalVal.trim();
        }
        return null;
    }

    private void persistResults(List<RuleBreachResult> breachingResults, RuleExecutionContextDto rule, TenantScenario tenantScenario) {
        for (RuleBreachResult result : breachingResults) {
            UUID customerId = result.getCustomer().getId();

            double geoMultiplier = geoRiskEvaluator.getGeographicRiskMultiplier(customerId);
            int finalRiskScore = (int) Math.min(100, Math.round(rule.getBaseRiskScore() * geoMultiplier));
            AlertSeverity finalSeverity = determineSeverity(finalRiskScore, rule.getSeverity());

            Alert alert = new Alert();
            alert.setAlertReference(buildAlertReference());
            alert.setTenantScenarioId(tenantScenario.getId());
            alert.setGlobalScenarioId(tenantScenario.getGlobalScenarioId());
            alert.setGlobalRuleId(rule.getGlobalRuleId());
            alert.setTenantRuleId(rule.getTenantRuleId());
            alert.setCustomerProfileId(customerId);
            alert.setSeverity(finalSeverity);
            alert.setRiskScore(BigDecimal.valueOf(finalRiskScore));
            alert.setTypologyTriggered(rule.getTypologyLabel());

            if (result.getTransactions() != null && !result.getTransactions().isEmpty()) {
                for (Transaction txn : result.getTransactions()) {
                    if (txn != null) {
                        alert.addAlertTransaction(txn);
                    }
                }
            }

            Alert savedAlert = alertRepo.save(alert);

            List<AlertEvidence> evidences = rule.getConditions().stream().map(cond -> {
                AlertEvidence evidence = new AlertEvidence();
                evidence.setAlert(savedAlert);
                evidence.setAttributeName(cond.getAttributeName());
                evidence.setAggregationFunction(cond.getAggregationFunction());
                evidence.setOperator(cond.getOperator());

                String appliedParams = String.format("%s over %s >= %s",
                        cond.getAggregationFunction(),
                        cond.getLookbackPeriod() != null ? cond.getLookbackPeriod() : "N/A",
                        cond.getThresholdValue());

                evidence.setThresholdApplied(appliedParams);
                evidence.setActualEvaluatedValue("EXCEEDED_THRESHOLD");
                return evidence;
            }).toList();

            evidenceRepo.saveAll(evidences);

            if (savedAlert.getAlertTransactions() != null && !savedAlert.getAlertTransactions().isEmpty()) {
                log.info("Alert {} created: Linked {} transactions for Customer {}",
                        savedAlert.getAlertReference(), savedAlert.getAlertTransactions().size(), customerId);
            } else {
                log.warn("Alert {} created WITHOUT transactions. This might cause issues in STR filing.",
                        savedAlert.getAlertReference());
            }
        }
    }

    private String buildAlertReference() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randomNum = new Random().nextInt(90000) + 10000;
        return "ALT-" + dateStr + "-" + randomNum;
    }

    private AlertSeverity determineSeverity(int score, AlertSeverity baseSeverity) {
        if (score >= 85) return AlertSeverity.CRITICAL;
        if (score >= 70) return AlertSeverity.HIGH;
        if (score >= 40) return AlertSeverity.MEDIUM;
        return AlertSeverity.LOW;
    }
}