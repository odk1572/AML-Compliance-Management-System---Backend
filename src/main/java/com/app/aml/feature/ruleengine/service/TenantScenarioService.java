package com.app.aml.feature.ruleengine.service;

import com.app.aml.feature.ruleengine.dto.globalScenario.response.GlobalScenarioResponseDto;
import com.app.aml.feature.ruleengine.dto.tenantRule.response.TenantRuleResponseDto;
import com.app.aml.feature.ruleengine.dto.tenantScenario.response.TenantScenarioResponseDto;
import com.app.aml.feature.ruleengine.dto.tenantScenario.response.TenantScenarioWithRulesDto;

import java.util.List;
import java.util.UUID;

public interface TenantScenarioService {
    TenantScenarioResponseDto activateScenario(UUID globalScenarioId);
    TenantScenarioResponseDto pauseScenario(UUID tenantScenarioId);
    List<TenantScenarioWithRulesDto> listActiveScenariosWithRules();
    TenantScenarioWithRulesDto getScenarioByIdWithRules(UUID tenantScenarioId);
    TenantRuleResponseDto toggleScenarioRule(UUID tenantRuleId, boolean isActive);
    List<GlobalScenarioResponseDto> getAvailableGlobalScenarios();
    TenantScenarioResponseDto resumeScenario(UUID tenantScenarioId);
}
