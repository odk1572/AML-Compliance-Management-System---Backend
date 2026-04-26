package com.app.aml.feature.ruleengine.dto.tenantScenario.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTenantScenarioRequestDto {

    @NotNull(message = "Global Scenario ID is required")
    private UUID globalScenarioId;

    @NotNull(message = "Initial status is required")
    @Pattern(regexp = "ACTIVE|PAUSED", message = "Status must be either ACTIVE or PAUSED")
    private String status;
}