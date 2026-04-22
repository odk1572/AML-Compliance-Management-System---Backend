package com.app.aml.feature.casemanagement.dto.caseEscalation.request;

import com.app.aml.domain.enums.EscalationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCaseEscalationStatusDto {

    @NotNull(message = "Escalation status is required")
    private EscalationStatus escalationStatus;
}