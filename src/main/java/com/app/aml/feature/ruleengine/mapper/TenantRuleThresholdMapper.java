package com.app.aml.feature.ruleengine.mapper;

import com.app.aml.feature.ruleengine.dto.tenantRuleThreshold.request.CreateTenantRuleThresholdRequestDto;
import com.app.aml.feature.ruleengine.dto.tenantRuleThreshold.response.TenantRuleThresholdResponseDto;
import com.app.aml.feature.ruleengine.dto.tenantRuleThreshold.request.UpdateTenantRuleThresholdRequestDto;
import com.app.aml.feature.ruleengine.entity.TenantRule;
import com.app.aml.feature.ruleengine.entity.TenantRuleThreshold;
import org.mapstruct.*;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TenantRuleThresholdMapper {

    @Mapping(target = "tenantRuleId", source = "tenantRule.id")
    TenantRuleThresholdResponseDto toResponseDto(TenantRuleThreshold entity);

    List<TenantRuleThresholdResponseDto> toResponseDtoList(List<TenantRuleThreshold> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantRule", source = "tenantRuleId")
    TenantRuleThreshold toEntity(CreateTenantRuleThresholdRequestDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantRule", ignore = true) // Protect relationship from accidental overwrite
    @Mapping(target = "globalConditionId", ignore = true) // Condition association is immutable
    void updateEntityFromDto(UpdateTenantRuleThresholdRequestDto dto, @MappingTarget TenantRuleThreshold entity);

    /**
     * Maps a UUID from the DTO to a JPA proxy/entity for TenantRule.
     */
    default TenantRule mapTenantRuleIdToRule(UUID tenantRuleId) {
        if (tenantRuleId == null) {
            return null;
        }
        TenantRule rule = new TenantRule();
        rule.setId(tenantRuleId);
        return rule;
    }
}