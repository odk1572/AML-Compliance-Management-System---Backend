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

    @NotBlank(message = "Aggregation function is required")
    @Size(max = 10, message = "Aggregation function cannot exceed 10 characters")
    private String aggregationFunction;

    @Size(max = 10, message = "Lookback period cannot exceed 10 characters")
    private String lookbackPeriod;


    @NotBlank(message = "Threshold value is required")
    @Size(max = 255, message = "Threshold value cannot exceed 255 characters")
    private String thresholdValue;

}