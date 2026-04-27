package com.app.aml.feature.ruleengine.dto.globalRules.response;

import com.app.aml.enums.AlertSeverity;
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
public class GlobalRuleResponseDto {
    private UUID id;
    private String ruleName;
    private String ruleType;
    private AlertSeverity severity;

    private Integer baseRiskScore;

    private Instant sysCreatedAt;
    private Instant sysUpdatedAt;
    private Boolean sysIsDeleted;
    private Instant sysDeletedAt;
}