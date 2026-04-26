package com.app.aml.feature.ruleengine.dto.tenantScenario.response;

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
public class TenantScenarioResponseDto {
    private UUID id;
    private UUID globalScenarioId;
    private String status;
    private UUID sysActivatedBy;

    // From AuditableEntity
    private Instant sysCreatedAt;
    private Instant sysUpdatedAt;
}