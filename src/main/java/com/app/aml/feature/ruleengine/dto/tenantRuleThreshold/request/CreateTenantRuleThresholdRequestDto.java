package com.app.aml.feature.ruleengine.dto.tenantRuleThreshold.request;

import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "Tenant Rule reference code is required")
    private String tenantRuleCode;

    private UUID globalConditionId; 

    @NotBlank(message = "Global Condition reference code is required")
    private String globalConditionCode;

    @Size(max = 255, message = "Override value cannot exceed 255 characters")
    private String overrideValue;

    @Size(max = 10, message = "Override lookback period cannot exceed 10 characters")
    private String overrideLookbackPeriod;
}