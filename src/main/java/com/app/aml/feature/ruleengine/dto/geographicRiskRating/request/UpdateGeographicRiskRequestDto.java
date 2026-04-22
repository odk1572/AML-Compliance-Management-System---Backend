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
public class UpdateGeographicRiskRequestDto {

    @NotBlank(message = "Country name is required")
    private String countryName;

    @NotBlank(message = "FATF status is required")
    private String fatfStatus;

    @NotNull(message = "Basel AML Index Score is required")
    @Min(0) @Max(100)
    private Integer baselAmlIndexScore;

    @NotBlank(message = "Risk tier is required")
    private String riskTier;

    private String notes;

    @NotNull(message = "Effective date is required")
    private Instant effectiveFrom;
}