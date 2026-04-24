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

    @NotBlank(message = "Threshold applied is required")
    @Size(max = 255, message = "Threshold applied cannot exceed 255 characters")
    private String thresholdApplied;

    @NotBlank(message = "Actual evaluated value is required")
    @Size(max = 255, message = "Actual evaluated value cannot exceed 255 characters")
    private String actualEvaluatedValue;
}
