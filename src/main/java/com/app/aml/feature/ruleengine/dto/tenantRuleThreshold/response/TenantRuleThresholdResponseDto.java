package com.app.aml.feature.ruleengine.dto.tenantRuleThreshold.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TenantRuleThresholdResponseDto {
    private UUID id;
    private UUID tenantRuleId;
    private UUID globalConditionId;
    private String overrideValue;
    private String overrideLookbackPeriod;
    private String overrideAggregationFunction;

}