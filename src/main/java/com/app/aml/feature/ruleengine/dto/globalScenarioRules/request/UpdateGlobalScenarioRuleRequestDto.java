package com.app.aml.feature.ruleengine.dto.globalScenarioRules.request;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGlobalScenarioRuleRequestDto {

    @NotNull(message = "Active status is required")
    private Boolean isActive;

    @NotNull(message = "Priority order is required")
    @Min(value = 0, message = "Priority order cannot be negative")
    private Integer priorityOrder;
}