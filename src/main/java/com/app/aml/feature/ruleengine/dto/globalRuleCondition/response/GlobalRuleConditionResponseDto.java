package com.app.aml.feature.ruleengine.dto.globalRuleCondition.response;

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
public class GlobalRuleConditionResponseDto {

    private UUID id;

    private UUID ruleId; // Flattened for the API

    private String attributeName;

    private String thresholdValue;

    private String valueDataType;

    private Instant sysCreatedAt;

    private Instant sysUpdatedAt;
}