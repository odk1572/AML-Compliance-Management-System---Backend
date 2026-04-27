package com.app.aml.feature.casemanagement.dto.caseAlertLink.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CaseAlertLinkResponseDto {
    private UUID id;
    private UUID caseId;
    private UUID alertId;
    private UUID linkedBy;
    private boolean isPrimaryAlert;
    private Instant sysCreatedAt;
}