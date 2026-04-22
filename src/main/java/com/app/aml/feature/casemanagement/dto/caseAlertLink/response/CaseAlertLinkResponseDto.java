package com.app.aml.feature.casemanagement.dto.caseAlertLink.response;

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
public class CaseAlertLinkResponseDto {
    private UUID id;
    private UUID caseId;   // Flattened from CaseRecord
    private UUID alertId;  // Flattened from Alert
    private UUID linkedBy;
    private boolean isPrimaryAlert;
    private Instant sysCreatedAt;
}