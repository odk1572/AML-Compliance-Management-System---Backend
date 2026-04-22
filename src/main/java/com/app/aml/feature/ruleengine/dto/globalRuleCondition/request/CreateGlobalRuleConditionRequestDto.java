package com.app.aml.feature.ruleengine.dto.globalRuleCondition.request;

import jakarta.validation.constraints.Min;
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
public class CreateGlobalRuleConditionRequestDto {

    @NotNull(message = "Rule ID is required to associate this condition")
    private UUID ruleId;

    @NotBlank(message = "Attribute name (e.g., TXN_AMOUNT) is required")
    @Size(max = 100)
    private String attributeName;

    @NotNull(message = "Sequence order is required")
    @Min(1)
    private Integer conditionSequence;

    @NotBlank(message = "Aggregation function (e.g., SUM, COUNT, NONE) is required")
    private String aggregationFunction;

    private String lookbackPeriod; // e.g., "P1D" (ISO-8601 Duration) or "24H"

    @NotBlank(message = "Operator (e.g., GREATER_THAN, EQUALS) is required")
    private String operator;

    @NotBlank(message = "Threshold value is required")
    private String thresholdValue;

    @NotBlank(message = "Data type (e.g., DECIMAL, STRING) is required")
    private String valueDataType;
}