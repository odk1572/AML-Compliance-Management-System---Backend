package com.app.aml.feature.ruleengine.dto.tenantScenario.request;

import com.app.aml.domain.enums.RuleStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateTenantScenarioStatusDto {
    @NotNull(message = "Status is required")
    private RuleStatus status;
}