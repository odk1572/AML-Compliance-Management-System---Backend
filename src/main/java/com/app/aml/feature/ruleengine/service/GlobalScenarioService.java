package com.app.aml.feature.ruleengine.service;

import com.app.aml.feature.ruleengine.dto.globalScenario.request.CreateGlobalScenarioRequestDto;
import com.app.aml.feature.ruleengine.dto.globalScenario.request.UpdateGlobalScenarioRequestDto;
import com.app.aml.feature.ruleengine.dto.globalScenario.response.GlobalScenarioResponseDto;
import com.app.aml.feature.ruleengine.dto.globalScenarioRules.request.UpdateGlobalScenarioRuleRequestDto;
import com.app.aml.feature.ruleengine.dto.globalScenarioRules.response.GlobalScenarioRuleResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface GlobalScenarioService {

    GlobalScenarioResponseDto createScenario(CreateGlobalScenarioRequestDto dto);

    GlobalScenarioResponseDto updateScenario(UUID id, UpdateGlobalScenarioRequestDto dto);

    void deleteScenario(UUID id);

    GlobalScenarioResponseDto getScenarioById(UUID id);

    Page<GlobalScenarioResponseDto> listScenarios(Pageable pageable);

    GlobalScenarioRuleResponseDto addRuleToScenario(UUID scenarioId, UUID ruleId, Integer priority);

    void removeRuleFromScenario(UUID scenarioId, UUID ruleId);

    List<GlobalScenarioRuleResponseDto> getRulesByScenarioId(UUID scenarioId);

    GlobalScenarioRuleResponseDto updateRuleInScenario(UUID scenarioId, UUID ruleId, UpdateGlobalScenarioRuleRequestDto dto);
}