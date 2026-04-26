package com.app.aml.feature.ruleengine.dto.globalRules.response;

import com.app.aml.domain.enums.AlertSeverity;
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
public class GlobalRuleResponseDto {
    private UUID id;
    private String ruleName;
    private String ruleType;
    private AlertSeverity severity;

    // Changed to Integer to match the 'int' type in GlobalRule entity
    private Integer baseRiskScore;

    // Audit fields from AuditableEntity
    private Instant sysCreatedAt;
    private Instant sysUpdatedAt;

    // Soft delete fields inherited from SoftDeletableEntity
    private Boolean sysIsDeleted;
    private Instant sysDeletedAt;
}