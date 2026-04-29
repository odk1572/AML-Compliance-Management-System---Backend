package com.app.aml.feature.ruleengine.dto.globalRuleCondition.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GlobalRuleConditionResponseDto {

    private UUID id;

    private String conditionCode;

    private UUID ruleId;

    private String attributeName;

    private String thresholdValue;

    private String valueDataType;

    private Instant sysCreatedAt;

    private Instant sysUpdatedAt;
}