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
public class ScenarioExecutionDto {
    private String scenarioCode;
    private String logicalOperator; // "AND" or "OR"
    private List<RuleExecutionDto> activeRules;
}
