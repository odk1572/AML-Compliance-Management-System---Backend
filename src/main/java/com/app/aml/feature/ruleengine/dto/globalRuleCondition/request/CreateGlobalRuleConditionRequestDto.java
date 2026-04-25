package com.app.aml.feature.ruleengine.dto.globalRuleCondition.request;

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

    @NotBlank(message = "Attribute name is required")
    @Size(max = 100, message = "Attribute name cannot exceed 100 characters")
    private String attributeName;

    @NotBlank(message = "Threshold value is required")
    @Size(max = 255, message = "Threshold value cannot exceed 255 characters")
    private String thresholdValue;

    @NotBlank(message = "Value data type is required")
    @Size(max = 50, message = "Value data type cannot exceed 50 characters")
    private String valueDataType;

}