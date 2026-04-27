package com.app.aml.feature.ruleengine.dto.geographicRiskRating.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeographicRiskRatingResponseDto {
    private String countryCode;
    private String countryName;
    private String fatfStatus;
    private Integer baselAmlIndexScore;
    private String riskTier;
    private String notes;
    private Instant effectiveFrom;
    private Instant sysCreatedAt;
    private Instant sysUpdatedAt;
}