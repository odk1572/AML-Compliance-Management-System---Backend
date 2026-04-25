package com.app.aml.feature.ruleengine.dto.tenantScenario.response;

import com.app.aml.feature.ruleengine.dto.tenantRule.response.TenantRuleResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class TenantScenarioWithRulesDto {
    private TenantScenarioResponseDto scenario;
    private List<TenantRuleResponseDto> rules;
}