package com.app.aml.feature.casemanagement.dto.caseAlertLink.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCaseAlertLinkRequestDto {

    @NotNull(message = "Case ID is required")
    private UUID caseId;

    @NotNull(message = "Alert ID is required")
    private UUID alertId;

    @NotNull(message = "Primary alert status is required")
    private Boolean isPrimaryAlert;
}