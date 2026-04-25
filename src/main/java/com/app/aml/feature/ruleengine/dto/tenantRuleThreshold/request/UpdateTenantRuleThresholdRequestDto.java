package com.app.aml.feature.ruleengine.dto.tenantRuleThreshold.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTenantRuleThresholdRequestDto {

    @Size(max = 255, message = "Override value cannot exceed 255 characters")
    private String overrideValue;

}