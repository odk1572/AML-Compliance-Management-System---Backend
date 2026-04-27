package com.app.aml.feature.ruleengine.dto.tenantScenario.response;

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
public class TenantScenarioResponseDto {
    private UUID id;
    private UUID globalScenarioId;
    private String status;
    private UUID sysActivatedBy;

    private Instant sysCreatedAt;
    private Instant sysUpdatedAt;
}