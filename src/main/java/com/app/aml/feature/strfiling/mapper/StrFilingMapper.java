package com.app.aml.feature.strfiling.mapper;

import com.app.aml.feature.casemanagement.entity.CaseRecord;
import com.app.aml.feature.strfiling.dto.strFiling.CreateStrFilingRequestDto;
import com.app.aml.feature.strfiling.dto.strFiling.StrFilingResponseDto;
import com.app.aml.feature.strfiling.entity.StrFiling;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StrFilingMapper {

    @Mapping(target = "caseId", source = "caseRecord.id")
    StrFilingResponseDto toResponseDto(StrFiling entity);

    List<StrFilingResponseDto> toResponseDtoList(List<StrFiling> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sysCreatedAt", ignore = true)
    @Mapping(target = "filingReference", ignore = true)
    @Mapping(target = "filedBy", ignore = true)
    @Mapping(target = "caseRecord", source = "caseId")
    StrFiling toEntity(CreateStrFilingRequestDto dto);

    default CaseRecord mapCaseIdToCaseRecord(UUID caseId) {
        if (caseId == null) {
            return null;
        }
        CaseRecord caseRecord = new CaseRecord();
        caseRecord.setId(caseId);
        return caseRecord;
    }
}