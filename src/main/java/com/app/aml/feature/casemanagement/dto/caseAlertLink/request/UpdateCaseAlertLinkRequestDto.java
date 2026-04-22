package com.app.aml.feature.casemanagement.dto.caseAlertLink.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCaseAlertLinkRequestDto {

    @NotNull(message = "Primary alert status is required")
    private Boolean isPrimaryAlert;
}