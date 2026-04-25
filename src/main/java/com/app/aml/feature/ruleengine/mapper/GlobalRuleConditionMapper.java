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
    GlobalRuleConditionResponseDto toResponseDto(GlobalRuleCondition entity);

    List<GlobalRuleConditionResponseDto> toResponseDtoList(List<GlobalRuleCondition> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rule", source = "ruleId")
        // Note: sysCreatedAt, sysUpdatedAt, sysCreatedBy, etc., are safely ignored
        // automatically by unmappedTargetPolicy = ReportingPolicy.IGNORE
    GlobalRuleCondition toEntity(CreateGlobalRuleConditionRequestDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rule", ignore = true) // Protects the relationship from being overwritten on update
    void updateEntityFromDto(UpdateGlobalRuleConditionRequestDto dto, @MappingTarget GlobalRuleCondition entity);

    // Helper method to turn a UUID from the JSON payload into a JPA proxy entity
    default GlobalRule mapRuleIdToGlobalRule(UUID ruleId) {
        if (ruleId == null) {
            return null;
        }
        GlobalRule proxyRule = new GlobalRule();
        proxyRule.setId(ruleId);
        return proxyRule;
    }
}