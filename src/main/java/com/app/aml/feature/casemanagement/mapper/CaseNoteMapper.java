package com.app.aml.feature.casemanagement.mapper;

import com.app.aml.feature.casemanagement.dto.caseNote.CaseNoteResponseDto;
import com.app.aml.feature.casemanagement.dto.request.CaseNoteRequestDto;
import com.app.aml.feature.casemanagement.entity.CaseNote;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CaseNoteMapper {

    // Maps the nested CaseRecord's ID to the flat caseId field in the Response DTO
    @Mapping(target = "caseId", source = "caseRecord.id")
    CaseNoteResponseDto toResponseDto(CaseNote entity);

    List<CaseNoteResponseDto> toResponseDtoList(List<CaseNote> entities);

    // Only maps noteType and noteContent from the Request DTO.
    // Ignores the rest because the Service layer sets them manually.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sysCreatedAt", ignore = true)
    @Mapping(target = "authoredBy", ignore = true)
    @Mapping(target = "caseRecord", ignore = true)
    CaseNote toEntity(CaseNoteRequestDto dto);
}