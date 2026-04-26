package com.app.aml.feature.ruleengine.dto.globalScenario.response;

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
public class GlobalScenarioResponseDto {
    private UUID id;
    private String scenarioName;
    private String category;
    private String description;
    private UUID createdBy;
    private Instant sysCreatedAt;
    private Instant sysUpdatedAt;
    private Boolean sysIsDeleted;
    private Instant sysDeletedAt;
}