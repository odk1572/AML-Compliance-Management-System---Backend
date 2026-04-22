package com.app.aml.feature.ruleengine.dto.globalRuleCondition.request;

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
public class UpdateGlobalRuleConditionRequestDto {

    @NotBlank(message = "Attribute name is required")
    @Size(max = 100)
    private String attributeName;

    @NotNull(message = "Sequence order is required")
    @Min(1)
    private Integer conditionSequence;

    @NotBlank(message = "Aggregation function is required")
    private String aggregationFunction;

    private String lookbackPeriod;

    @NotBlank(message = "Operator is required")
    private String operator;

    @NotBlank(message = "Threshold value is required")
    private String thresholdValue;

    @NotBlank(message = "Data type is required")
    private String valueDataType;
}