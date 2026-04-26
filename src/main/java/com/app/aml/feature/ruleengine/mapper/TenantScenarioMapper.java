package com.app.aml.feature.ruleengine.mapper;

import com.app.aml.feature.ruleengine.dto.tenantScenario.request.CreateTenantScenarioRequestDto;
import com.app.aml.feature.ruleengine.dto.tenantScenario.request.UpdateTenantScenarioStatusDto;
import com.app.aml.feature.ruleengine.dto.tenantScenario.response.TenantScenarioResponseDto;
import com.app.aml.feature.ruleengine.entity.TenantScenario;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TenantScenarioMapper {

    TenantScenarioResponseDto toResponseDto(TenantScenario entity);

    List<TenantScenarioResponseDto> toResponseDtoList(List<TenantScenario> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sysActivatedBy", ignore = true)
    @Mapping(target = "sysCreatedAt", ignore = true)
    @Mapping(target = "sysUpdatedAt", ignore = true)
    TenantScenario toEntity(CreateTenantScenarioRequestDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "globalScenarioId", ignore = true)
    @Mapping(target = "sysActivatedBy", ignore = true)
    @Mapping(target = "sysCreatedAt", ignore = true)
    @Mapping(target = "sysUpdatedAt", ignore = true)
    void updateStatus(UpdateTenantScenarioStatusDto dto, @MappingTarget TenantScenario entity);
}