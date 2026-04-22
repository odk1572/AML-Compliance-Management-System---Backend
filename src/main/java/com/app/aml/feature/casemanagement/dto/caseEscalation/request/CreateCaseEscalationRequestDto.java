package com.app.aml.feature.casemanagement.dto.caseEscalation.request;

import jakarta.validation.constraints.NotBlank;
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
public class CreateCaseEscalationRequestDto {

    @NotNull(message = "Case ID is required")
    private UUID caseId;

    @NotNull(message = "The user being escalated to is required")
    private UUID escalatedTo;

    @NotBlank(message = "Escalation reason is required")
    private String escalationReason;

    // escalationStatus defaults to PENDING in the entity, so it is omitted here.
}