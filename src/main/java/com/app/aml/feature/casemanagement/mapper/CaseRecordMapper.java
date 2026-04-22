package com.app.aml.feature.casemanagement.mapper;

import com.app.aml.feature.casemanagement.dto.caseRecord.response.CaseResponseDto;
import com.app.aml.feature.casemanagement.dto.caseRecord.request.CloseCaseRequestDto;
import com.app.aml.feature.casemanagement.dto.caseRecord.request.CreateCaseRequestDto;
import com.app.aml.feature.casemanagement.dto.caseRecord.request.UpdateCaseRequestDto;
import com.app.aml.feature.casemanagement.entity.CaseRecord;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CaseRecordMapper {

    CaseResponseDto toResponseDto(CaseRecord entity);

    List<CaseResponseDto> toResponseDtoList(List<CaseRecord> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "caseReference", ignore = true)
    @Mapping(target = "openedAt", ignore = true)
    @Mapping(target = "lastActivityAt", ignore = true)
    @Mapping(target = "status", constant = "OPEN")
    CaseRecord toEntity(CreateCaseRequestDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "caseReference", ignore = true)
    @Mapping(target = "openedAt", ignore = true)
    void updateEntityFromDto(UpdateCaseRequestDto dto, @MappingTarget CaseRecord entity);

    @Mapping(target = "status", constant = "CLOSED")
    void closeCase(CloseCaseRequestDto dto, @MappingTarget CaseRecord entity);
}
