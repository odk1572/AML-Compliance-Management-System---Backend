package com.app.aml.feature.ruleengine.dto.tenantScenario.request;

import com.app.aml.domain.enums.RuleStatus;
import jakarta.validation.constraints.NotNull;
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
    private RuleStatus status;
}