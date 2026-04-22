package com.app.aml.feature.ruleengine.dto.tenantRuleThreshold.response;

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
public class TenantRuleThresholdResponseDto {
    private UUID id;
    private UUID tenantRuleId;
    private UUID globalConditionId;
    private String overrideValue;
    private String overrideLookbackPeriod;
    private String overrideAggregationFunction;
    private Instant sysCreatedAt;
    private Instant sysUpdatedAt;
}