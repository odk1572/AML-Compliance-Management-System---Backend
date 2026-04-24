package com.app.aml.feature.ruleengine.service;


import com.app.aml.feature.ruleengine.dto.globalRuleCondition.request.CreateGlobalRuleConditionRequestDto;
import com.app.aml.feature.ruleengine.dto.globalRuleCondition.request.UpdateGlobalRuleConditionRequestDto;
import com.app.aml.feature.ruleengine.dto.globalRuleCondition.response.GlobalRuleConditionResponseDto;
import com.app.aml.feature.ruleengine.dto.globalRules.request.CreateGlobalRuleRequestDto;
import com.app.aml.feature.ruleengine.dto.globalRules.request.UpdateGlobalRuleRequestDto;
import com.app.aml.feature.ruleengine.dto.globalRules.response.GlobalRuleResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface GlobalRuleService {

    GlobalRuleResponseDto createRule(CreateGlobalRuleRequestDto dto);

    GlobalRuleResponseDto updateRule(UUID id, UpdateGlobalRuleRequestDto dto);

    void deleteRule(UUID id);

    GlobalRuleResponseDto getRuleById(UUID id);

    Page<GlobalRuleResponseDto> listRules(Pageable pageable);

    Page<Map<String, Object>> listRulesWithAlertCounts(Pageable pageable);

    GlobalRuleConditionResponseDto addConditionToRule(CreateGlobalRuleConditionRequestDto dto);

    GlobalRuleConditionResponseDto updateCondition(UUID conditionId, UpdateGlobalRuleConditionRequestDto dto);

    void removeCondition(UUID conditionId);

    List<GlobalRuleConditionResponseDto> getConditionsByRuleId(UUID ruleId);
}