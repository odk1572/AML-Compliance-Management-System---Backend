package com.app.aml.feature.casemanagement.mapper;

import com.app.aml.feature.casemanagement.dto.caseAuditTrail.CaseAuditTrailResponseDto;
import com.app.aml.feature.casemanagement.entity.CaseAuditTrail;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CaseAuditTrailMapper {

    @Mapping(target = "caseId", source = "caseRecord.id")
    CaseAuditTrailResponseDto toResponseDto(CaseAuditTrail entity);

    List<CaseAuditTrailResponseDto> toResponseDtoList(List<CaseAuditTrail> entities);
}