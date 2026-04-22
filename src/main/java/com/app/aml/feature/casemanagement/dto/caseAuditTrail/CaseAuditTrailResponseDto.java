package com.app.aml.feature.casemanagement.dto.caseAuditTrail;

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
public class CaseAuditTrailResponseDto {
    private UUID id;
    private UUID caseId; // Flattened from CaseRecord
    private UUID actorId;
    private String eventType;
    private String eventMetadata; // Contains the JSON payload of what changed
    private String ipAddress;
    private Instant sysCreatedAt;
}