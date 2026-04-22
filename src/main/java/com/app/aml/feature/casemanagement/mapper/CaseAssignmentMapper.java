package com.app.aml.feature.casemanagement.mapper;

import com.app.aml.feature.casemanagement.dto.caseAssignment.response.CaseAssignmentResponseDto;
import com.app.aml.feature.casemanagement.dto.caseAssignment.request.CreateCaseAssignmentRequestDto;
import com.app.aml.feature.casemanagement.entity.CaseAssignment;
import com.app.aml.feature.casemanagement.entity.CaseRecord;
import org.mapstruct.*;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CaseAssignmentMapper {
   @Mapping(target = "caseId", source = "caseRecord.id")
    CaseAssignmentResponseDto toResponseDto(CaseAssignment entity);

    List<CaseAssignmentResponseDto> toResponseDtoList(List<CaseAssignment> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sysCreatedAt", ignore = true)
    @Mapping(target = "assignedBy", ignore = true)
    @Mapping(target = "assignedFrom", ignore = true)
    @Mapping(target = "caseRecord", source = "caseId")
    CaseAssignment toEntity(CreateCaseAssignmentRequestDto dto);

    default CaseRecord mapCaseIdToCaseRecord(UUID caseId) {
        if (caseId == null) {
            return null;
        }
        CaseRecord caseRecord = new CaseRecord();
        caseRecord.setId(caseId);
        return caseRecord;
    }
}