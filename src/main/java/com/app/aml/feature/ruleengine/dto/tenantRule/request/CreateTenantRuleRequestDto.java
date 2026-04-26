package com.app.aml.feature.ruleengine.dto.tenantRule.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTenantRuleRequestDto {

    @NotNull(message = "Tenant Scenario ID is required")
    private UUID tenantScenarioId;

    @NotNull(message = "Global Rule ID is required")
    private UUID globalRuleId;

    @NotBlank(message = "Rule code is required")
    @Size(max = 50, message = "Rule code cannot exceed 50 characters")
    private String ruleCode;

    @NotBlank(message = "Rule name is required")
    @Size(max = 255, message = "Rule name cannot exceed 255 characters")
    private String ruleName;

    @NotNull(message = "Active status is required")
    private Boolean isActive;
}