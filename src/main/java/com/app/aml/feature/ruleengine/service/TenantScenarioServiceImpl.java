package com.app.aml.feature.ruleengine.service;

import com.app.aml.enums.RuleStatus;
import com.app.aml.feature.ruleengine.dto.tenantRule.response.TenantRuleResponseDto;
import com.app.aml.feature.ruleengine.dto.tenantScenario.response.TenantScenarioResponseDto;
import com.app.aml.feature.ruleengine.dto.tenantScenario.response.TenantScenarioWithRulesDto;
import com.app.aml.feature.ruleengine.entity.GlobalScenarioRule;
import com.app.aml.feature.ruleengine.entity.TenantRule;
import com.app.aml.feature.ruleengine.entity.TenantScenario;
import com.app.aml.feature.ruleengine.mapper.TenantRuleMapper;
import com.app.aml.feature.ruleengine.mapper.TenantScenarioMapper;
import com.app.aml.feature.ruleengine.repository.GlobalScenarioRuleRepository;
import com.app.aml.feature.ruleengine.repository.TenantRuleRepository;
import com.app.aml.feature.ruleengine.repository.TenantScenarioRepository;
import com.app.aml.shared.audit.service.AuditLogService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantScenarioServiceImpl implements TenantScenarioService {

    private final TenantScenarioRepository tenantScenarioRepo;
    private final TenantRuleRepository tenantRuleRepo;
    private final GlobalScenarioRuleRepository globalScenarioRuleRepo;

    private final TenantScenarioMapper tenantScenarioMapper;
    private final TenantRuleMapper tenantRuleMapper;

    private final AuditLogService auditLogService;

    @Transactional
    @Override
    public TenantScenarioResponseDto activateScenario(UUID globalScenarioId) {
        log.info("Activating Global Scenario ID: {} for tenant", globalScenarioId);

        if (tenantScenarioRepo.existsByGlobalScenarioId(globalScenarioId)) {
            throw new IllegalStateException("Scenario is already activated for this tenant.");
        }

        TenantScenario tenantScenario = new TenantScenario();
        tenantScenario.setGlobalScenarioId(globalScenarioId);
        // Fix: Convert Enum to String
        tenantScenario.setStatus(RuleStatus.ACTIVE);

        TenantScenario savedScenario = tenantScenarioRepo.save(tenantScenario);

        List<GlobalScenarioRule> globalRules = globalScenarioRuleRepo.findByScenarioId(globalScenarioId);

        List<TenantRule> tenantRules = globalRules.stream()
                .filter(GlobalScenarioRule::isActive)
                .map(globalLink -> {
                    TenantRule rule = new TenantRule();
                    rule.setTenantScenario(savedScenario);
                    rule.setGlobalRuleId(globalLink.getRule().getId());
                    rule.setRuleName(globalLink.getRule().getRuleName());
                    rule.setRuleCode(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                    rule.setActive(true);
                    return rule;
                })
                .collect(Collectors.toList());

        tenantRuleRepo.saveAll(tenantRules);
        log.info("Successfully activated scenario and seeded {} tenant rules.", tenantRules.size());

        TenantScenarioResponseDto response = tenantScenarioMapper.toResponseDto(savedScenario);

        auditLogService.log(
                null,
                "TENANT_SCENARIO",
                "ACTIVATE",
                "TenantScenario",
                savedScenario.getId(),
                null,
                response
        );

        return response;
    }

    @Transactional
    @Override
    public TenantScenarioResponseDto pauseScenario(UUID tenantScenarioId) {
        log.info("Pausing Tenant Scenario ID: {}", tenantScenarioId);

        TenantScenario scenario = tenantScenarioRepo.findById(tenantScenarioId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant Scenario not found"));

        RuleStatus oldStatus = scenario.getStatus();
        scenario.setStatus(RuleStatus.PAUSED);
        TenantScenario savedScenario = tenantScenarioRepo.save(scenario);

        TenantScenarioResponseDto response = tenantScenarioMapper.toResponseDto(savedScenario);

        auditLogService.log(
                null,
                "TENANT_SCENARIO",
                "PAUSE",
                "TenantScenario",
                scenario.getId(),
                "{\"status\":\"" + oldStatus + "\"}",
                "{\"status\":\"" + RuleStatus.PAUSED.name() + "\"}"
        );

        return response;
    }

    @Transactional
    @Override
    public TenantScenarioResponseDto resumeScenario(UUID tenantScenarioId) {
        log.info("Resuming Tenant Scenario ID: {}", tenantScenarioId);

        TenantScenario scenario = tenantScenarioRepo.findById(tenantScenarioId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant Scenario not found"));

        if (RuleStatus.ACTIVE.equals(scenario.getStatus())) {
            throw new IllegalStateException("Scenario is already active.");
        }

        RuleStatus oldStatus = scenario.getStatus();
        scenario.setStatus(RuleStatus.ACTIVE);
        TenantScenario savedScenario = tenantScenarioRepo.save(scenario);

        TenantScenarioResponseDto response = tenantScenarioMapper.toResponseDto(savedScenario);

        auditLogService.log(
                null,
                "TENANT_SCENARIO",
                "RESUME",
                "TenantScenario",
                scenario.getId(),
                "{\"status\":\"" + oldStatus + "\"}",
                "{\"status\":\"" + RuleStatus.ACTIVE.name() + "\"}"
        );

        return response;
    }

    @Transactional
    @Override
    public TenantRuleResponseDto toggleScenarioRule(UUID tenantRuleId, boolean isActive) {
        log.info("Toggling Tenant Rule ID: {} to active: {}", tenantRuleId, isActive);

        TenantRule rule = tenantRuleRepo.findById(tenantRuleId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant Rule not found"));

        boolean oldStatus = rule.isActive();
        rule.setActive(isActive);
        TenantRule savedRule = tenantRuleRepo.save(rule);

        TenantRuleResponseDto response = tenantRuleMapper.toResponseDto(savedRule);

        auditLogService.log(
                null,
                "TENANT_RULE",
                "TOGGLE",
                "TenantRule",
                rule.getId(),
                "{\"isActive\":" + oldStatus + "}",
                "{\"isActive\":" + isActive + "}"
        );

        return response;
    }

    @Transactional(readOnly = true)
    @Override
    public TenantScenarioWithRulesDto getScenarioByIdWithRules(UUID tenantScenarioId) {
        log.debug("Fetching scenario and rules for ID: {}", tenantScenarioId);

        TenantScenario scenario = tenantScenarioRepo.findById(tenantScenarioId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant Scenario not found"));

        List<TenantRule> rules = tenantRuleRepo.findByTenantScenarioId(tenantScenarioId);

        return TenantScenarioWithRulesDto.builder()
                .scenario(tenantScenarioMapper.toResponseDto(scenario))
                .rules(tenantRuleMapper.toResponseDtoList(rules))
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public List<TenantScenarioWithRulesDto> listActiveScenariosWithRules() {
        log.debug("Fetching all active tenant scenarios with their rules");

        List<TenantScenario> activeScenarios = tenantScenarioRepo.findByStatus(RuleStatus.ACTIVE);

        return activeScenarios.stream().map(scenario -> {
            List<TenantRule> rules = tenantRuleRepo.findByTenantScenarioId(scenario.getId());

            return TenantScenarioWithRulesDto.builder()
                    .scenario(tenantScenarioMapper.toResponseDto(scenario))
                    .rules(tenantRuleMapper.toResponseDtoList(rules))
                    .build();
        }).collect(Collectors.toList());
    }
}