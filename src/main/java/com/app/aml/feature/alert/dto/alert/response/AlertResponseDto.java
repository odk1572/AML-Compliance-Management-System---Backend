package com.app.aml.feature.alert.dto.alert.response;

import com.app.aml.enums.AlertSeverity;
import com.app.aml.enums.AlertStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlertResponseDto {
    private UUID id;
    private UUID customerProfileId;
    private UUID tenantScenarioId;
    private UUID globalScenarioId;
    private UUID globalRuleId;
    private UUID tenantRuleId;
    private String alertReference;
    private AlertSeverity severity;
    private AlertStatus status;
    private String typologyTriggered;
    private BigDecimal riskScore;
    private Instant sysCreatedAt;
    private Instant sysUpdatedAt;
}