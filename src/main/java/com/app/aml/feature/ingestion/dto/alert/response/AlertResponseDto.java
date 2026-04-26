package com.app.aml.feature.ingestion.dto.alert.response;

import com.app.aml.domain.enums.AlertSeverity;
import com.app.aml.domain.enums.AlertStatus;
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
public class AlertResponseDto {
    private UUID id;
    private UUID customerProfileId;
    private UUID transactionId;
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