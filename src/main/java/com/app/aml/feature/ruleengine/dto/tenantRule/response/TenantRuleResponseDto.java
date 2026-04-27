package com.app.aml.feature.ruleengine.dto.tenantRule.response;

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
public class TenantRuleResponseDto {
    private UUID id;
    private UUID tenantScenarioId;
    private UUID globalRuleId;
    private String ruleCode;
    private String ruleName;
    private boolean isActive;

    // Audit & Soft Delete Info
    private UUID sysCreatedBy;
    private Instant sysCreatedAt;
    private Instant sysUpdatedAt;
    private Boolean sysIsDeleted;
    private Instant sysDeletedAt;
}