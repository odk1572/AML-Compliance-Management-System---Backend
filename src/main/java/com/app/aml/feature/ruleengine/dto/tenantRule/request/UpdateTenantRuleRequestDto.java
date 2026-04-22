package com.app.aml.feature.ruleengine.dto.tenantRule.request;

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
public class UpdateTenantRuleRequestDto {

    @NotBlank(message = "Rule name is required")
    @Size(max = 255, message = "Rule name cannot exceed 255 characters")
    private String ruleName;

    @NotNull(message = "Active status is required")
    private Boolean isActive;
}