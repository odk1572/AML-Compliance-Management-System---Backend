package com.app.aml.feature.ingestion.dto.alertEvidence.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAlertEvidenceRequestDto {

    @NotNull(message = "Alert ID is required")
    private UUID alertId;

    @NotBlank(message = "Attribute name is required")
    @Size(max = 100)
    private String attributeName; // e.g., 'Single Transaction Limit'

    @NotBlank(message = "Aggregation function is required")
    @Size(max = 10)
    private String aggregationFunction; // e.g., 'SUM', 'COUNT'

    @NotBlank(message = "Operator is required")
    @Size(max = 30)
    private String operator; // e.g., 'GREATER_THAN'

    @NotBlank(message = "Threshold applied is required")
    @Size(max = 255)
    private String thresholdApplied;

    @NotBlank(message = "Actual evaluated value is required")
    @Size(max = 255)
    private String actualEvaluatedValue;
}