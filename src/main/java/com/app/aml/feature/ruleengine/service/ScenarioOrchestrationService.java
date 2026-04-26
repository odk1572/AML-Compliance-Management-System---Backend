package com.app.aml.feature.ruleengine.service;

import com.app.aml.domain.enums.AlertSeverity;
import com.app.aml.feature.ingestion.entity.Alert;
import com.app.aml.feature.ingestion.entity.AlertEvidence;
import com.app.aml.feature.ingestion.repository.AlertEvidenceRepository;
import com.app.aml.feature.ingestion.repository.AlertRepository;
import com.app.aml.feature.ingestion.repository.AlertTransactionRepository;
import com.app.aml.feature.ingestion.repository.TransactionRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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
    public Set<UUID> executeFullScenario(UUID tenantScenarioId) {
        Set<UUID> scenarioBreaches = new HashSet<>();
        List<TenantRuleResponseDto> activeRules = loadActiveRules(tenantScenarioId);

        for (TenantRuleResponseDto tenantRule : activeRules) {
            GlobalRule globalRule = globalRuleRepo.findById(tenantRule.getGlobalRuleId())
                    .orElseThrow(() -> new IllegalArgumentException("Global Rule not found"));

            List<GlobalRuleCondition> conditions = conditionRepo.findByRuleId(globalRule.getId());
            List<TenantRuleThreshold> thresholds = thresholdRepo.findByTenantRuleId(tenantRule.getId());

            RuleExecutionContextDto context = buildExecutionContext(tenantRule, globalRule, conditions, thresholds);
            RuleExecutorStrategy strategy = executorFactory.getStrategy(context.getRuleType());

            Set<UUID> breachingCustomers = strategy.executeRule(context);

            if (!breachingCustomers.isEmpty()) {
                persistResults(breachingCustomers, context, tenantScenarioId);
                scenarioBreaches.addAll(breachingCustomers);
            }
        }

        return scenarioBreaches;
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

        String aggFn = (override != null && override.getOverrideAggregationFunction() != null)
                ? override.getOverrideAggregationFunction() : cond.getAggregationFunction();

        String lookback = (override != null && override.getOverrideLookbackPeriod() != null)
                ? override.getOverrideLookbackPeriod() : cond.getLookbackPeriod();

        String threshold = (override != null && override.getOverrideValue() != null)
                ? override.getOverrideValue() : cond.getThresholdValue();

        return ConditionExecutionContextDto.builder()
                .attributeName(cond.getAttributeName())
                .aggregationFunction(aggFn)
                .lookbackPeriod(lookback)
                .thresholdValue(threshold)
                .operator("BREACHED")
                .build();
    }

    private void persistResults(Set<UUID> customerIds, RuleExecutionContextDto rule, UUID tenantScenarioId) {
        for (UUID customerId : customerIds) {
            double geoMultiplier = geoRiskEvaluator.getGeographicRiskMultiplier(customerId);
            int finalRiskScore = (int) Math.min(100, Math.round(rule.getBaseRiskScore() * geoMultiplier));
            AlertSeverity finalSeverity = determineSeverity(finalRiskScore, rule.getSeverity());

            Alert alert = new Alert();

            alert.setId(UUID.randomUUID());
            alert.setAlertReference(buildAlertReference());
            alert.setTenantScenarioId(tenantScenarioId);
            alert.setGlobalScenarioId(rule.getGlobalRuleId());
            alert.setGlobalRuleId(rule.getGlobalRuleId());
            alert.setTenantRuleId(rule.getTenantRuleId());
            alert.setCustomerProfileId(customerId);
            alert.setSeverity(finalSeverity);
            alert.setRiskScore(BigDecimal.valueOf(finalRiskScore));
            alert.setTypologyTriggered(rule.getTypologyLabel());

            alertRepo.save(alert);

            List<AlertEvidence> evidences = rule.getConditions().stream().map(cond -> {
                AlertEvidence evidence = new AlertEvidence();
                evidence.setId(UUID.randomUUID());
                evidence.setAlert(alert);
                evidence.setThresholdApplied(cond.getThresholdValue());
                evidence.setActualEvaluatedValue("EXCEEDED_THRESHOLD");
                return evidence;
            }).toList();

            evidenceRepo.saveAll(evidences);
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