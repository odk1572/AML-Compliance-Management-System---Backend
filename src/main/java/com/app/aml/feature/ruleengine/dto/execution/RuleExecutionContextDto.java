package com.app.aml.feature.ruleengine.dto.execution;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class RuleExecutionContextDto {
    private UUID ruleId;
    private String ruleName;
    private String ruleCategory;
    private Integer baseRiskScore;
    private List<ConditionExecutionContextDto> conditions;
}