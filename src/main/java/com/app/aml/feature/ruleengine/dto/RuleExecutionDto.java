package com.app.aml.feature.ruleengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleExecutionDto {
    private String ruleCode;
    private String ruleType;
    private List<ConditionExecutionDto> conditions;
}
