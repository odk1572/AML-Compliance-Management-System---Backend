package com.app.aml.feature.ruleengine.dto.geographicRiskRating.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGeographicRiskRequestDto {

    @NotBlank(message = "Country code is required")
    @Size(min = 2, max = 3, message = "Country code must be ISO Alpha-2 or Alpha-3")
    private String countryCode;

    @NotBlank(message = "Country name is required")
    private String countryName;

    @NotBlank(message = "FATF status is required")
    private String fatfStatus;

    @NotNull(message = "Basel AML Index Score is required")
    @Min(0) @Max(100)
    private Integer baselAmlIndexScore;

    @NotBlank(message = "Risk tier is required")
    private String riskTier; // e.g., "LOW", "MEDIUM", "HIGH"

    private String notes;

    @NotNull(message = "Effective date is required")
    private Instant effectiveFrom;
}