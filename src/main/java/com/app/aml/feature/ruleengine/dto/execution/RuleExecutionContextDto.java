package com.app.aml.feature.ruleengine.dto.execution;

import com.app.aml.domain.enums.AlertSeverity;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class RuleExecutionContextDto {
    private UUID tenantRuleId;
    private UUID globalRuleId;
    private String ruleType;
    private AlertSeverity severity;
    private int baseRiskScore;
    private String typologyLabel;
    private List<ConditionExecutionContextDto> conditions;
}