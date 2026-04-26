package com.app.aml.feature.ruleengine.dto.tenantRuleThreshold.request;

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
public class CreateTenantRuleThresholdRequestDto {

    @NotNull(message = "Tenant Rule ID is required")
    private UUID tenantRuleId;

    @NotNull(message = "Global Condition ID is required")
    private UUID globalConditionId;

    @Size(max = 255, message = "Override value cannot exceed 255 characters")
    private String overrideValue;

    @Size(max = 10, message = "Override lookback period cannot exceed 10 characters")
    private String overrideLookbackPeriod;

    @Size(max = 10, message = "Override aggregation function cannot exceed 10 characters")
    private String overrideAggregationFunction;
}