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
    public Set<UUID> executeFullScenario(UUID tenantScenarioId) {
        log.info("Starting execution for Tenant Scenario ID: {}", tenantScenarioId);

        Set<UUID> scenarioBreaches = new HashSet<>();

        // Fetch the parent scenario to correctly map Alert hierarchy later
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
            Set<UUID> breachingCustomers = strategy.executeRule(context);

            if (!breachingCustomers.isEmpty()) {
                log.info("Rule [{}] triggered for {} customers.", tenantRule.getRuleName(), breachingCustomers.size());
                persistResults(breachingCustomers, context, tenantScenario);
                scenarioBreaches.addAll(breachingCustomers);
            }
        }

        log.info("Scenario execution complete. Total unique customers flagged: {}", scenarioBreaches.size());
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

        // Use resolveSafe to prevent empty strings ("") from breaking the executors
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

    /**
     * Safely falls back to the global value if the override is null OR completely blank.
     */
    private String resolveSafe(String overrideVal, String globalVal) {
        if (overrideVal != null && !overrideVal.isBlank()) {
            return overrideVal.trim();
        }
        if (globalVal != null && !globalVal.isBlank()) {
            return globalVal.trim();
        }
        return null;
    }

    private void persistResults(Set<UUID> customerIds, RuleExecutionContextDto rule, TenantScenario tenantScenario) {
        for (UUID customerId : customerIds) {
            double geoMultiplier = geoRiskEvaluator.getGeographicRiskMultiplier(customerId);
            int finalRiskScore = (int) Math.min(100, Math.round(rule.getBaseRiskScore() * geoMultiplier));
            AlertSeverity finalSeverity = determineSeverity(finalRiskScore, rule.getSeverity());

            Alert alert = new Alert();

            // REMOVED alert.setId(UUID.randomUUID());
            // Let JPA generate the ID automatically to prevent the "SELECT before INSERT" behavior!

            alert.setAlertReference(buildAlertReference());

            // THE FIX: Set the initial status of the Alert!
            // (Change AlertStatus.NEW to whatever your actual Enum/Constant is named)
            // alert.setStatus(AlertStatus.NEW);  <-- UNCOMMENT AND UPDATE THIS LINE

            // Fixed Hierarchical Mapping
            alert.setTenantScenarioId(tenantScenario.getId());

            // Fixed typo: Ensure this points to the Global Scenario, not the Tenant Scenario
            alert.setGlobalScenarioId(tenantScenario.getGlobalScenarioId()); // or .getScenarioId() depending on your entity

            alert.setGlobalRuleId(rule.getGlobalRuleId());
            alert.setTenantRuleId(rule.getTenantRuleId());

            alert.setCustomerProfileId(customerId);
            alert.setSeverity(finalSeverity);
            alert.setRiskScore(BigDecimal.valueOf(finalRiskScore));
            alert.setTypologyTriggered(rule.getTypologyLabel());
            alert.setTransaction(null);

            alertRepo.save(alert);

            List<AlertEvidence> evidences = rule.getConditions().stream().map(cond -> {
                AlertEvidence evidence = new AlertEvidence();

                        evidence.setAttributeName(cond.getAttributeName());
                evidence.setAggregationFunction(cond.getAggregationFunction());
                evidence.setOperator(cond.getOperator()); // This was "BREACHED" in buildExecutionContext
                // --- MANDATORY FIELDS END ---

                evidence.setAlert(alert);

                // Construct a readable evidence string (e.g., "COUNT over 7d >= 10")
                String appliedParams = String.format("%s over %s >= %s",
                        cond.getAggregationFunction(),
                        cond.getLookbackPeriod() != null ? cond.getLookbackPeriod() : "N/A",
                        cond.getThresholdValue());

                evidence.setThresholdApplied(appliedParams);
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