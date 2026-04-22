package com.app.aml.feature.tenantuser.mapper;

import com.app.aml.feature.tenantuser.dto.CreateTenantUserRequestDto;
import com.app.aml.feature.tenantuser.dto.TenantUserResponseDto;
import com.app.aml.feature.tenantuser.dto.UpdateTenantUserRequestDto;
import com.app.aml.feature.tenantuser.entity.TenantUser;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TenantUserMapper {

    TenantUserResponseDto toResponseDto(TenantUser entity);

    List<TenantUserResponseDto> toResponseDtoList(List<TenantUser> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", source = "password")
    @Mapping(target = "sysCreatedBy", ignore = true)
    TenantUser toEntity(CreateTenantUserRequestDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employeeId", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "sysCreatedBy", ignore = true)
    void updateEntityFromDto(UpdateTenantUserRequestDto dto, @MappingTarget TenantUser entity);
}