package com.app.aml.feature.ruleengine.mapper;

import com.app.aml.feature.ruleengine.dto.globalRules.request.CreateGlobalRuleRequestDto;
import com.app.aml.feature.ruleengine.dto.globalRules.response.GlobalRuleResponseDto;
import com.app.aml.feature.ruleengine.dto.globalRules.request.UpdateGlobalRuleRequestDto;
import com.app.aml.feature.ruleengine.entity.GlobalRule;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GlobalRuleMapper {

    GlobalRuleResponseDto toResponseDto(GlobalRule entity);

    List<GlobalRuleResponseDto> toResponseDtoList(List<GlobalRule> entities);

    @Mapping(target = "id", ignore = true)
        // Inherited SoftDeletableEntity/AuditableEntity fields are safely ignored
        // because of ReportingPolicy.IGNORE
    GlobalRule toEntity(CreateGlobalRuleRequestDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(UpdateGlobalRuleRequestDto dto, @MappingTarget GlobalRule entity);
}