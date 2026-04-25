package com.app.aml.feature.ruleengine.dto.globalScenarioRules.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
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
public class CreateGlobalScenarioRuleRequestDto {

    @NotNull(message = "Scenario ID is required")
    private UUID scenarioId;

    @NotNull(message = "Rule ID is required")
    private UUID ruleId;

    @NotNull(message = "Active status is required")
    @JsonProperty("isActive") // Prevents Jackson from renaming this to "active" in JSON
    private Boolean isActive;

    @NotNull(message = "Priority order is required")
    @Min(value = 0, message = "Priority order cannot be negative")
    private Integer priorityOrder;
}