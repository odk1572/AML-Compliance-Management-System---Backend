package com.app.aml.feature.ruleengine.dto.execution;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConditionExecutionContextDto {
    private String attributeName;
    private String aggregationFunction;
    private String operator;
    private String thresholdValue;
    private String lookbackPeriod;
}