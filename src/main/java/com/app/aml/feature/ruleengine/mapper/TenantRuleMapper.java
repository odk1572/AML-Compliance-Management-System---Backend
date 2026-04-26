package com.app.aml.feature.ruleengine.mapper;

import com.app.aml.feature.ruleengine.dto.tenantRule.request.CreateTenantRuleRequestDto;
import com.app.aml.feature.ruleengine.dto.tenantRule.request.UpdateTenantRuleRequestDto;
import com.app.aml.feature.ruleengine.dto.tenantRule.response.TenantRuleResponseDto;
import com.app.aml.feature.ruleengine.entity.TenantRule;
import com.app.aml.feature.ruleengine.entity.TenantScenario;
import org.mapstruct.*;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TenantRuleMapper {

    @Mapping(target = "tenantScenarioId", source = "tenantScenario.id")
    TenantRuleResponseDto toResponseDto(TenantRule entity);

    List<TenantRuleResponseDto> toResponseDtoList(List<TenantRule> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sysCreatedBy", ignore = true)
    @Mapping(target = "sysCreatedAt", ignore = true)
    @Mapping(target = "sysUpdatedAt", ignore = true)
    @Mapping(target = "sysIsDeleted", ignore = true)
    @Mapping(target = "sysDeletedAt", ignore = true)
    @Mapping(target = "tenantScenario", source = "tenantScenarioId")
    TenantRule toEntity(CreateTenantRuleRequestDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sysCreatedBy", ignore = true)
    @Mapping(target = "sysCreatedAt", ignore = true)
    @Mapping(target = "sysUpdatedAt", ignore = true)
    @Mapping(target = "sysIsDeleted", ignore = true)
    @Mapping(target = "sysDeletedAt", ignore = true)
    @Mapping(target = "tenantScenario", ignore = true)
    @Mapping(target = "globalRuleId", ignore = true)
    @Mapping(target = "ruleCode", ignore = true)
    void updateEntityFromDto(UpdateTenantRuleRequestDto dto, @MappingTarget TenantRule entity);

    /**
     * Maps a UUID from the DTO to a JPA proxy/entity for TenantScenario.
     */
    default TenantScenario mapTenantScenarioIdToScenario(UUID tenantScenarioId) {
        if (tenantScenarioId == null) {
            return null;
        }
        TenantScenario scenario = new TenantScenario();
        scenario.setId(tenantScenarioId);
        return scenario;
    }
}