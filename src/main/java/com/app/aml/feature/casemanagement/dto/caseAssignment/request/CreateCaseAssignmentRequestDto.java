package com.app.aml.feature.casemanagement.dto.caseAssignment.request;

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
public class CreateCaseAssignmentRequestDto {

    @NotNull(message = "Case ID is required")
    private UUID caseId;

    @NotNull(message = "The user being assigned to is required")
    private UUID assignedTo;

    private String assignmentReason;
}