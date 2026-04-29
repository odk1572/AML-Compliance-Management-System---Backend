package com.app.aml.feature.ruleengine.mapper;

import com.app.aml.feature.ruleengine.dto.globalRuleCondition.request.CreateGlobalRuleConditionRequestDto;
import com.app.aml.feature.ruleengine.dto.globalRuleCondition.response.GlobalRuleConditionResponseDto;
import com.app.aml.feature.ruleengine.dto.globalRuleCondition.request.UpdateGlobalRuleConditionRequestDto;
import com.app.aml.feature.ruleengine.entity.GlobalRule;
import com.app.aml.feature.ruleengine.entity.GlobalRuleCondition;
import org.mapstruct.*;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GlobalRuleConditionMapper {

    @Mapping(target = "ruleId", source = "rule.id")
    @Mapping(source = "lookbackPeriod", target = "lookbackPeriod")
    @Mapping(source = "conditionCode", target = "conditionCode")
    GlobalRuleConditionResponseDto toResponseDto(GlobalRuleCondition entity);

    List<GlobalRuleConditionResponseDto> toResponseDtoList(List<GlobalRuleCondition> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sysCreatedAt", ignore = true)
    @Mapping(target = "sysUpdatedAt", ignore = true)
    @Mapping(target = "rule", source = "ruleId")
    GlobalRuleCondition toEntity(CreateGlobalRuleConditionRequestDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sysCreatedAt", ignore = true)
    @Mapping(target = "sysUpdatedAt", ignore = true)
    @Mapping(target = "rule", ignore = true)
    void updateEntityFromDto(UpdateGlobalRuleConditionRequestDto dto, @MappingTarget GlobalRuleCondition entity);

    default GlobalRule mapRuleIdToGlobalRule(UUID ruleId) {
        if (ruleId == null) {
            return null;
        }
        GlobalRule proxyRule = new GlobalRule();
        proxyRule.setId(ruleId);
        return proxyRule;
    }
}