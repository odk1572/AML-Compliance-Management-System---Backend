package com.app.aml.feature.casemanagement.dto.caseAssignment.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseAssignmentResponseDto {
    private UUID id;
    private UUID caseId; // Flattened from CaseRecord
    private UUID assignedFrom;
    private UUID assignedTo;
    private UUID assignedBy;
    private String assignmentReason;
    private Instant sysCreatedAt;
}