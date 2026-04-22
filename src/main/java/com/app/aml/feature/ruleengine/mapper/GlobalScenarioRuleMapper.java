package com.app.aml.feature.ruleengine.mapper;


import com.app.aml.feature.ruleengine.dto.globalScenarioRules.request.CreateGlobalScenarioRuleRequestDto;
import com.app.aml.feature.ruleengine.dto.globalScenarioRules.response.GlobalScenarioRuleResponseDto;
import com.app.aml.feature.ruleengine.dto.globalScenarioRules.request.UpdateGlobalScenarioRuleRequestDto;
import com.app.aml.feature.ruleengine.entity.GlobalRule;
import com.app.aml.feature.ruleengine.entity.GlobalScenario;
import com.app.aml.feature.ruleengine.entity.GlobalScenarioRule;
import org.mapstruct.*;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GlobalScenarioRuleMapper {


    @Mapping(target = "scenarioId", source = "scenario.id")
    @Mapping(target = "ruleId", source = "rule.id")
    GlobalScenarioRuleResponseDto toResponseDto(GlobalScenarioRule entity);

    List<GlobalScenarioRuleResponseDto> toResponseDtoList(List<GlobalScenarioRule> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sysCreatedAt", ignore = true)
    @Mapping(target = "scenario", source = "scenarioId")
    @Mapping(target = "rule", source = "ruleId")
    GlobalScenarioRule toEntity(CreateGlobalScenarioRuleRequestDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sysCreatedAt", ignore = true)
    @Mapping(target = "scenario", ignore = true)
    @Mapping(target = "rule", ignore = true)
    void updateEntityFromDto(UpdateGlobalScenarioRuleRequestDto dto, @MappingTarget GlobalScenarioRule entity);

    default GlobalScenario mapScenarioIdToScenario(UUID scenarioId) {
        if (scenarioId == null) {
            return null;
        }
        GlobalScenario scenario = new GlobalScenario();
        scenario.setId(scenarioId);
        return scenario;
    }

    default GlobalRule mapRuleIdToRule(UUID ruleId) {
        if (ruleId == null) {
            return null;
        }
        GlobalRule rule = new GlobalRule();
        rule.setId(ruleId);
        return rule;
    }
}