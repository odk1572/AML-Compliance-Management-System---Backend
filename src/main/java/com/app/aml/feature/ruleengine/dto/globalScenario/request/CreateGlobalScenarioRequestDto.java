package com.app.aml.feature.ruleengine.dto.globalScenario.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGlobalScenarioRequestDto {

    @NotBlank(message = "Scenario name is required")
    @Size(max = 255, message = "Scenario name cannot exceed 255 characters")
    private String scenarioName;

    @NotBlank(message = "Category is required")
    @Size(max = 100, message = "Category cannot exceed 100 characters")
    private String category;

    private String description;
}