package com.app.aml.feature.ingestion.mapper;

import com.app.aml.feature.ingestion.dto.alertEvidence.request.CreateAlertEvidenceRequestDto;
import com.app.aml.feature.ingestion.dto.alertEvidence.response.AlertEvidenceResponseDto;
import com.app.aml.feature.ingestion.entity.Alert;
import com.app.aml.feature.ingestion.entity.AlertEvidence;
import org.mapstruct.*;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AlertEvidenceMapper {

    @Mapping(target = "alertId", source = "alert.id")
    AlertEvidenceResponseDto toResponseDto(AlertEvidence entity);

    List<AlertEvidenceResponseDto> toResponseDtoList(List<AlertEvidence> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sysCreatedAt", ignore = true)
    @Mapping(target = "alert", source = "alertId")
    AlertEvidence toEntity(CreateAlertEvidenceRequestDto dto);

    /**
     * Maps a UUID to a JPA Alert proxy to avoid unnecessary database lookups
     * when creating evidence records.
     */
    default Alert mapAlertIdToAlert(UUID alertId) {
        if (alertId == null) {
            return null;
        }
        Alert alert = new Alert();
        alert.setId(alertId);
        return alert;
    }
}