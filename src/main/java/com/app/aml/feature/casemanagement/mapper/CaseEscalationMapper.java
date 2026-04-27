package com.app.aml.feature.casemanagement.mapper;

import com.app.aml.feature.casemanagement.dto.caseEscalation.request.EscalationRequestDto;
import com.app.aml.feature.casemanagement.dto.caseEscalation.response.CaseEscalationResponseDto;
import com.app.aml.feature.casemanagement.dto.caseEscalation.request.UpdateCaseEscalationStatusDto;
import com.app.aml.feature.casemanagement.entity.CaseEscalation;
import com.app.aml.feature.casemanagement.entity.CaseRecord;
import org.mapstruct.*;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CaseEscalationMapper {

    @Mapping(target = "caseId", source = "caseRecord.id")
    CaseEscalationResponseDto toResponseDto(CaseEscalation entity);

    List<CaseEscalationResponseDto> toResponseDtoList(List<CaseEscalation> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "escalatedBy", ignore = true)
    @Mapping(target = "escalationStatus", ignore = true) // Entity defaults to PENDING
    @Mapping(target = "acknowledgedAt", ignore = true)
    @Mapping(target = "resolvedAt", ignore = true)
    CaseEscalation toEntity(EscalationRequestDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "caseRecord", ignore = true)
    @Mapping(target = "escalatedBy", ignore = true)
    @Mapping(target = "escalatedTo", ignore = true)
    @Mapping(target = "escalationReason", ignore = true)
    @Mapping(target = "acknowledgedAt", ignore = true)
    @Mapping(target = "resolvedAt", ignore = true)
    void updateStatus(UpdateCaseEscalationStatusDto dto, @MappingTarget CaseEscalation entity);

    default CaseRecord mapCaseIdToCaseRecord(UUID caseId) {
        if (caseId == null) {
            return null;
        }
        CaseRecord caseRecord = new CaseRecord();
        caseRecord.setId(caseId);
        return caseRecord;
    }
}