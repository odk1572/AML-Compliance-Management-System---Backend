package com.app.aml.feature.alert.dto.alertEvidence.response;

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
public class AlertEvidenceResponseDto {
    private UUID id;
    private UUID alertId;
    private String attributeName;
    private String aggregationFunction;
    private String operator;
    private String thresholdApplied;
    private String actualEvaluatedValue;
    private Instant sysCreatedAt;
}