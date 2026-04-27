package com.app.aml.feature.alert.dto.alert.request;

import com.app.aml.enums.AlertSeverity;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAlertRequestDto {

    @NotNull(message = "Customer Profile ID is required")
    private UUID customerProfileId;

    @NotNull(message = "Transaction ID is required")
    private UUID transactionId;

    @NotNull(message = "Tenant Scenario ID is required")
    private UUID tenantScenarioId;

    @NotNull(message = "Global Scenario ID is required")
    private UUID globalScenarioId;

    @NotNull(message = "Global Rule ID is required")
    private UUID globalRuleId;

    private UUID tenantRuleId; // Optional

    @NotBlank(message = "Alert reference is required")
    @Size(max = 50)
    private String alertReference;

    @NotNull(message = "Severity is required")
    private AlertSeverity severity;

    @NotBlank(message = "Typology triggered is required")
    @Size(max = 255)
    private String typologyTriggered;

    @NotNull(message = "Risk score is required")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal riskScore;
}