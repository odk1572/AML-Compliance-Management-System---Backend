package com.app.aml.feature.tenant.mapper;

import com.app.aml.feature.tenant.dto.request.CreateTenantRequestDto;
import com.app.aml.feature.tenant.dto.response.TenantResponseDto;
import com.app.aml.feature.tenant.dto.request.UpdateTenantRequestDto;
import com.app.aml.feature.tenant.entity.Tenant;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TenantMapper {


    TenantResponseDto toResponseDto(Tenant entity);


    List<TenantResponseDto> toResponseDtoList(List<Tenant> entities);


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true) // Handled by entity default value
    @Mapping(target = "sysCreatedBy", ignore = true) // Handled by Service layer
    @Mapping(target = "sysDeletedBy", ignore = true)
    Tenant toEntity(CreateTenantRequestDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantCode", ignore = true)
    @Mapping(target = "schemaName", ignore = true)
    @Mapping(target = "sysCreatedBy", ignore = true)
    @Mapping(target = "sysDeletedBy", ignore = true)
    void updateEntityFromDto(UpdateTenantRequestDto dto, @MappingTarget Tenant entity);
}
