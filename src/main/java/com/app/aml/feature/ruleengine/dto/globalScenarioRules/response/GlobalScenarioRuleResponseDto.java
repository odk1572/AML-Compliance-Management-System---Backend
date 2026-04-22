package com.app.aml.feature.ruleengine.dto.globalScenarioRules.response;

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
public class GlobalScenarioRuleResponseDto {
    private UUID id;
    private UUID scenarioId;
    private UUID ruleId;
    private boolean isActive;
    private Integer priorityOrder;
    private Instant sysCreatedAt;
}