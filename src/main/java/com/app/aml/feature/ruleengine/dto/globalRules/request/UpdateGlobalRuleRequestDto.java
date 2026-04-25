package com.app.aml.feature.ruleengine.dto.globalRules.request;

import com.app.aml.domain.enums.AlertSeverity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGlobalRuleRequestDto {

    @NotBlank(message = "Rule name is required")
    @Size(max = 150, message = "Rule name cannot exceed 150 characters")
    private String ruleName;

    @NotBlank(message = "Rule type is required")
    @Size(max = 50, message = "Rule type cannot exceed 50 characters")
    private String ruleType;

    @NotNull(message = "Severity is required")
    private AlertSeverity severity;

    @NotNull(message = "Base risk score is required")
    @Min(value = 0, message = "Risk score cannot be less than 0")
    @Max(value = 100, message = "Risk score cannot exceed 100")
    private Short baseRiskScore;
}