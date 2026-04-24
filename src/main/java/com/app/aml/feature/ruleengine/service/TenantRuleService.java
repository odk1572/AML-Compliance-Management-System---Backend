package com.app.aml.feature.ruleengine.service;


import com.app.aml.feature.ruleengine.dto.tenantRule.request.UpdateTenantRuleRequestDto;
import com.app.aml.feature.ruleengine.dto.tenantRule.response.TenantRuleResponseDto;
import com.app.aml.feature.ruleengine.dto.tenantRuleThreshold.request.CreateTenantRuleThresholdRequestDto;
import com.app.aml.feature.ruleengine.dto.tenantRuleThreshold.request.UpdateTenantRuleThresholdRequestDto;
import com.app.aml.feature.ruleengine.dto.tenantRuleThreshold.response.TenantRuleThresholdResponseDto;

import java.util.List;
import java.util.UUID;

public interface TenantRuleService {
    TenantRuleResponseDto getRuleById(UUID ruleId);
    TenantRuleResponseDto updateRule(UUID ruleId, UpdateTenantRuleRequestDto dto);
    TenantRuleResponseDto toggleRule(UUID ruleId, boolean isActive);

    TenantRuleThresholdResponseDto createThresholdOverride(CreateTenantRuleThresholdRequestDto dto);
    TenantRuleThresholdResponseDto updateThresholdOverride(UUID thresholdId, UpdateTenantRuleThresholdRequestDto dto);
    List<TenantRuleThresholdResponseDto> getThresholdsForRule(UUID ruleId);
    void deleteThresholdOverride(UUID thresholdId);
    List<TenantRuleThresholdResponseDto> updateThresholds(UUID ruleId, List<CreateTenantRuleThresholdRequestDto> overrides);
}