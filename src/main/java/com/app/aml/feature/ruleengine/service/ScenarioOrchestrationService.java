package com.app.aml.feature.ruleengine.service;

import com.app.aml.enums.AlertSeverity;
import com.app.aml.enums.RuleStatus;
import com.app.aml.feature.alert.entity.Alert;
import com.app.aml.feature.alert.entity.AlertEvidence;
import com.app.aml.feature.alert.repository.AlertEvidenceRepository;
import com.app.aml.feature.alert.repository.AlertRepository;
import com.app.aml.feature.alert.repository.AlertTransactionRepository;
import com.app.aml.feature.ingestion.entity.Transaction;
import com.app.aml.feature.ingestion.repository.TransactionRepository;
import com.app.aml.feature.ruleengine.dto.RuleBreachResult;
import com.app.aml.feature.ruleengine.dto.ScenarioExecutionRequestDto;
import com.app.aml.feature.ruleengine.dto.execution.ConditionExecutionContextDto;
import com.app.aml.feature.ruleengine.dto.execution.RuleExecutionContextDto;
import com.app.aml.feature.ruleengine.dto.tenantRule.response.TenantRuleResponseDto;
import com.app.aml.feature.ruleengine.entity.*;
import com.app.aml.feature.ruleengine.executor.GeographicRiskEvaluator;
import com.app.aml.feature.ruleengine.executor.RuleExecutorFactory;
import com.app.aml.feature.ruleengine.executor.RuleExecutorStrategy;
import com.app.aml.feature.ruleengine.mapper.TenantRuleMapper;
import com.app.aml.feature.ruleengine.repository.*;
import com.app.aml.annotation.AuditAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
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
    @AuditAction(category = "RULE_ENGINE", action = "RUN_SCENARIO_EXECUTION", entityType = "SCENARIO")
    public List<RuleBreachResult> executeFullScenario(UUID tenantScenarioId, ScenarioExecutionRequestDto requestDto) {
        log.info("Starting execution for Tenant Scenario ID: {}", tenantScenarioId);

        Instant endOfBox = (requestDto != null && requestDto.getGlobalLookbackEnd() != null)
                ? requestDto.getGlobalLookbackEnd()
                : Instant.now();

        Instant startOfBox = (requestDto != null && requestDto.getGlobalLookbackStart() != null)
                ? requestDto.getGlobalLookbackStart()
                : endOfBox.minus(Duration.ofDays(30));

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

            Duration bufferDuration = calculateMaxLookback(conditions, thresholds);
            Instant dataFetchStart = startOfBox.minus(bufferDuration);

            RuleExecutionContextDto context = buildExecutionContext(tenantRule, globalRule, conditions, thresholds, startOfBox, endOfBox, dataFetchStart);
            RuleExecutorStrategy strategy = executorFactory.getStrategy(context.getRuleType());

            log.debug("Handing off to Strategy: {}", strategy.getRuleType());

            List<RuleBreachResult> breachingResults = strategy.executeRule(context);

            if (!breachingResults.isEmpty()) {
                log.info("Rule [{}] triggered for {} customers.", tenantRule.getRuleName(), breachingResults.size());
                persistResults(breachingResults, context, tenantScenario);
                allScenarioBreaches.addAll(breachingResults);
            }
        }

        log.info("Scenario execution complete. Total breaches flagged: {}", allScenarioBreaches.size());
        return allScenarioBreaches;
    }

    private Duration calculateMaxLookback(List<GlobalRuleCondition> conditions, List<TenantRuleThreshold> thresholds) {
        Map<UUID, TenantRuleThreshold> overrideMap = thresholds.stream()
                .collect(Collectors.toMap(TenantRuleThreshold::getGlobalConditionId, t -> t));

        long maxDays = 0;

        for (GlobalRuleCondition cond : conditions) {
            TenantRuleThreshold override = overrideMap.get(cond.getId());
            String lookbackStr = (override != null && override.getOverrideLookbackPeriod() != null)
                    ? override.getOverrideLookbackPeriod()
                    : cond.getLookbackPeriod();

            maxDays = Math.max(maxDays, parseDays(lookbackStr));
        }

        return Duration.ofDays(maxDays);
    }

    private long parseDays(String lookback) {
        if (lookback == null || lookback.isBlank()) return 0;
        try {
            String parts = lookback.toLowerCase().replaceAll("[^0-9]", "");
            long value = Long.parseLong(parts);
            if (lookback.contains("month")) return value * 30;
            if (lookback.contains("year")) return value * 365;
            return value;
        } catch (Exception e) {
            return 0;
        }
    }

    private List<TenantRuleResponseDto> loadActiveRules(UUID tenantScenarioId) {
        List<TenantRule> rules = tenantRuleRepo.findByTenantScenarioIdAndIsActiveTrue(tenantScenarioId);
        return tenantRuleMapper.toResponseDtoList(rules);
    }

    private RuleExecutionContextDto buildExecutionContext(TenantRuleResponseDto tenantRule, GlobalRule globalRule,
                                                          List<GlobalRuleCondition> conditions,
                                                          List<TenantRuleThreshold> thresholds,
                                                          Instant startOfBox, Instant endOfBox, Instant dataFetchStart) {
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
                .globalLookbackStart(startOfBox)
                .globalLookbackEnd(endOfBox)
                .dataFetchStart(dataFetchStart)
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

            double geoMultiplier = geoRiskEvaluator.getGeographicRiskMultiplier(
                    customerId,
                    rule.getGlobalLookbackStart(),
                    rule.getGlobalLookbackEnd()
            );

            int finalRiskScore = (int) Math.min(100, Math.round(rule.getBaseRiskScore() * geoMultiplier));
            AlertSeverity finalSeverity = determineSeverity(finalRiskScore, rule.getSeverity());

            Alert alert = new Alert();
            alert.setAlertReference(buildAlertReference());
            alert.setTenantScenarioId(tenantScenario.getId());
            alert.setGlobalScenarioId(tenantScenario.getGlobalScenarioId());
            alert.setGlobalRuleId(rule.getGlobalRuleId());
            alert.setTenantRuleId(rule.getTenantRuleId());

            alert.setCustomer(result.getCustomer());

            alert.setSeverity(finalSeverity);
            alert.setRiskScore(BigDecimal.valueOf(finalRiskScore));

            String type = result.getRuleType() != null ? result.getRuleType() : rule.getRuleType();
            String label = result.getRuleLabel() != null ? result.getRuleLabel() : rule.getTypologyLabel();

            alert.setRuleType(type);
            alert.setTypologyTriggered(label);

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
                log.info("Alert {} created: Linked {} transactions for Customer {} using Rule Type [{}] and Label [{}]",
                        savedAlert.getAlertReference(), savedAlert.getAlertTransactions().size(), customerId, type, label);
            } else {
                log.warn("Alert {} created WITHOUT transactions for Rule Type [{}].",
                        savedAlert.getAlertReference(), type);
            }
        }
    }



    @Transactional
    @AuditAction(category = "RULE_ENGINE", action = "RUN_ALL_SCENARIOS_EXECUTION", entityType = "TENANT")
    public List<RuleBreachResult> executeAllActiveScenarios(ScenarioExecutionRequestDto requestDto) {
        log.info("Starting batch execution for ALL active scenarios for current tenant");

        List<TenantScenario> activeScenarios = tenantScenarioRepo.findAllByStatus(RuleStatus.ACTIVE);

        if (activeScenarios == null || activeScenarios.isEmpty()) {
            log.info("No active scenarios found for current tenant. Exiting batch run.");
            return Collections.emptyList();
        }

        log.info("Found {} active scenarios to execute.", activeScenarios.size());
        List<RuleBreachResult> allCombinedBreaches = new ArrayList<>();

        for (TenantScenario scenario : activeScenarios) {
            try {
                log.info("--- Handing off Scenario ID: {} ---", scenario.getId());
                List<RuleBreachResult> scenarioBreaches = executeFullScenario(scenario.getId(), requestDto);
                allCombinedBreaches.addAll(scenarioBreaches);

            } catch (Exception e) {
                log.error("Failed to execute Scenario ID [{}]: {}", scenario.getId(), e.getMessage(), e);
            }
        }

        log.info("Batch execution complete. Total breaches flagged across all scenarios: {}", allCombinedBreaches.size());
        return allCombinedBreaches;
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