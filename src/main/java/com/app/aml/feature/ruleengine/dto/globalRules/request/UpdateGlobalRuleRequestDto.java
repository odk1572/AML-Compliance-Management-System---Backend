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
    @Size(max = 255)
    private String ruleName;

    @NotBlank(message = "Condition logic is required")
    @Size(max = 255)
    private String conditionLogic;

    @NotNull(message = "Severity is required")
    private AlertSeverity severity;

    @NotNull(message = "Base risk score is required")
    @Min(0) @Max(100)
    private Integer baseRiskScore;
}
