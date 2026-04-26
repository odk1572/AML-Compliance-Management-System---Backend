package com.app.aml.feature.ruleengine.dto.tenantScenario.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateTenantScenarioStatusDto {

    @NotNull(message = "Status is required")
    @Pattern(regexp = "ACTIVE|PAUSED", message = "Status must be either ACTIVE or PAUSED")
    private String status;
}