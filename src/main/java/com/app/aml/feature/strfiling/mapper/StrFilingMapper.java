package com.app.aml.feature.strfiling.mapper;

import com.app.aml.feature.casemanagement.entity.CaseRecord;
import com.app.aml.feature.strfiling.dto.strFiling.StrFilingRequestDto;
import com.app.aml.feature.strfiling.dto.strFiling.StrFilingResponseDto;
import com.app.aml.feature.strfiling.entity.StrFiling;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.UUID;
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StrFilingMapper {

    // Target the nested ID inside the CaseRecord entity using the second parameter
    @Mapping(target = "caseRecord.id", source = "caseId")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "filingReference", ignore = true)
    @Mapping(target = "sysCreatedAt", ignore = true)
    StrFiling toEntity(StrFilingRequestDto dto, UUID caseId);

    // For the Response DTO (mapping Entity -> DTO)
    @Mapping(target = "caseId", source = "caseRecord.id")
    StrFilingResponseDto toResponseDto(StrFiling entity);
}