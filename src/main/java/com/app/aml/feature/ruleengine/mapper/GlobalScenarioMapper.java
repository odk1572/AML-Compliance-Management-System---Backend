package com.app.aml.feature.ruleengine.mapper;


import com.app.aml.feature.ruleengine.dto.globalScenario.request.CreateGlobalScenarioRequestDto;
import com.app.aml.feature.ruleengine.dto.globalScenario.response.GlobalScenarioResponseDto;
import com.app.aml.feature.ruleengine.dto.globalScenario.request.UpdateGlobalScenarioRequestDto;
import com.app.aml.feature.ruleengine.entity.GlobalScenario;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GlobalScenarioMapper {


    GlobalScenarioResponseDto toResponseDto(GlobalScenario entity);

    List<GlobalScenarioResponseDto> toResponseDtoList(List<GlobalScenario> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    GlobalScenario toEntity(CreateGlobalScenarioRequestDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    void updateEntityFromDto(UpdateGlobalScenarioRequestDto dto, @MappingTarget GlobalScenario entity);
}