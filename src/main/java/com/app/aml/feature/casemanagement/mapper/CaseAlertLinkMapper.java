package com.app.aml.feature.casemanagement.mapper;

import com.app.aml.feature.casemanagement.dto.caseAlertLink.response.CaseAlertLinkResponseDto;
import com.app.aml.feature.casemanagement.dto.caseAlertLink.request.CreateCaseAlertLinkRequestDto;
import com.app.aml.feature.casemanagement.dto.caseAlertLink.request.UpdateCaseAlertLinkRequestDto;
import com.app.aml.feature.casemanagement.entity.CaseAlertLink;
import com.app.aml.feature.casemanagement.entity.CaseRecord;
import com.app.aml.feature.ingestion.entity.Alert;
import org.mapstruct.*;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CaseAlertLinkMapper {

    @Mapping(target = "caseId", source = "caseRecord.id")
    @Mapping(target = "alertId", source = "alert.id")
    CaseAlertLinkResponseDto toResponseDto(CaseAlertLink entity);

    List<CaseAlertLinkResponseDto> toResponseDtoList(List<CaseAlertLink> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "linkedBy", ignore = true)
    @Mapping(target = "sysCreatedAt", ignore = true)
    @Mapping(target = "caseRecord", source = "caseId")
    @Mapping(target = "alert", source = "alertId")
    CaseAlertLink toEntity(CreateCaseAlertLinkRequestDto dto);


   @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "caseRecord", ignore = true)
    @Mapping(target = "alert", ignore = true)
    @Mapping(target = "linkedBy", ignore = true)
    @Mapping(target = "sysCreatedAt", ignore = true)
    void updateEntityFromDto(UpdateCaseAlertLinkRequestDto dto, @MappingTarget CaseAlertLink entity);

    default CaseRecord mapCaseIdToCaseRecord(UUID caseId) {
        if (caseId == null) {
            return null;
        }
        CaseRecord caseRecord = new CaseRecord();
        caseRecord.setId(caseId);
        return caseRecord;
    }

    default Alert mapAlertIdToAlert(UUID alertId) {
        if (alertId == null) {
            return null;
        }
        Alert alert = new Alert();
        alert.setId(alertId);
        return alert;
    }
}