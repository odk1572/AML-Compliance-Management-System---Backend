package com.app.aml.feature.platformuser.mapper;

import com.app.aml.feature.platformuser.dto.CreatePlatformUserRequestDto;
import com.app.aml.feature.platformuser.dto.PlatformUserResponseDto;
import com.app.aml.feature.platformuser.dto.UpdatePlatformUserRequestDto;
import com.app.aml.feature.platformuser.entity.PlatformUser;
import org.mapstruct.*;


@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PlatformUserMapper {

    @Mapping(target = "locked", source = "locked")
    PlatformUserResponseDto toResponseDto(PlatformUser entity);

    @Mapping(target = "passwordHash", source = "password")
    @Mapping(target = "id", ignore = true)
    PlatformUser toEntity(CreatePlatformUserRequestDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    void updateEntityFromDto(UpdatePlatformUserRequestDto dto, @MappingTarget PlatformUser entity);
}