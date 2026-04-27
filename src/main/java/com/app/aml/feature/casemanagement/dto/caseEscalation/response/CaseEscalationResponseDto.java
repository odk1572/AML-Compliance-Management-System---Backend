package com.app.aml.feature.casemanagement.dto.caseEscalation.response;

import com.app.aml.enums.EscalationStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CaseEscalationResponseDto {
    private UUID id;
    private UUID caseId; // Flattened from CaseRecord
    private UUID escalatedBy;
    private UUID escalatedTo;
    private String escalationReason;
    private EscalationStatus escalationStatus;
    private Instant acknowledgedAt;
    private Instant resolvedAt;
    private Instant sysCreatedAt;
    private Instant sysUpdatedAt;
}