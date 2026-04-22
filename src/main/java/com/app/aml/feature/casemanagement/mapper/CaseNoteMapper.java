package com.app.aml.feature.casemanagement.mapper;


import com.app.aml.feature.casemanagement.dto.caseNote.CaseNoteResponseDto;
import com.app.aml.feature.casemanagement.dto.caseNote.CreateCaseNoteRequestDto;
import com.app.aml.feature.casemanagement.entity.CaseNote;
import com.app.aml.feature.casemanagement.entity.CaseRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CaseNoteMapper {

    @Mapping(target = "caseId", source = "caseRecord.id")
    CaseNoteResponseDto toResponseDto(CaseNote entity);

    List<CaseNoteResponseDto> toResponseDtoList(List<CaseNote> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sysCreatedAt", ignore = true)
    @Mapping(target = "authoredBy", ignore = true)
    @Mapping(target = "caseRecord", source = "caseId")
    CaseNote toEntity(CreateCaseNoteRequestDto dto);

    default CaseRecord mapCaseIdToCaseRecord(UUID caseId) {
        if (caseId == null) {
            return null;
        }
        CaseRecord caseRecord = new CaseRecord();
        caseRecord.setId(caseId);
        return caseRecord;
    }
}