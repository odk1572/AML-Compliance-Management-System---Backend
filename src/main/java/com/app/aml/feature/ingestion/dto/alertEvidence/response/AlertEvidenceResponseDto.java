package com.app.aml.feature.ingestion.dto.alertEvidence.response;

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
public class AlertEvidenceResponseDto {
    private UUID id;
    private UUID alertId; // Flattened from the Alert object
    private String attributeName;
    private String operator;
    private String thresholdApplied;
    private String actualEvaluatedValue;
    private Instant sysCreatedAt;
}