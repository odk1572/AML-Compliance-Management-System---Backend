package com.app.aml.feature.ruleengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConditionExecutionDto {
    private String aggregationFunction;
    private String attributeName;
    private String thresholdValue;
    private String lookbackPeriod;
}
