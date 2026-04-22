package com.app.aml.feature.ruleengine.mapper;

import com.app.aml.feature.ruleengine.dto.geographicRiskRating.request.CreateGeographicRiskRequestDto;
import com.app.aml.feature.ruleengine.dto.geographicRiskRating.response.GeographicRiskRatingResponseDto;
import com.app.aml.feature.ruleengine.dto.geographicRiskRating.request.UpdateGeographicRiskRequestDto;
import com.app.aml.feature.ruleengine.entity.GeographicRiskRating;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GeographicRiskRatingMapper {


    @Mapping(target = "baselAmlIndexScore", source = "baselAmlIndexSAcore")
    GeographicRiskRatingResponseDto toResponseDto(GeographicRiskRating entity);

    List<GeographicRiskRatingResponseDto> toResponseDtoList(List<GeographicRiskRating> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "baselAmlIndexSAcore", source = "baselAmlIndexScore")
    GeographicRiskRating toEntity(CreateGeographicRiskRequestDto dto);


    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "countryCode", ignore = true) // Country code usually shouldn't change
    @Mapping(target = "baselAmlIndexSAcore", source = "baselAmlIndexScore")
    void updateEntityFromDto(UpdateGeographicRiskRequestDto dto, @MappingTarget GeographicRiskRating entity);
}