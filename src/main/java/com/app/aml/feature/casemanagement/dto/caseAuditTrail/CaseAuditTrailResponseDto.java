package com.app.aml.feature.casemanagement.dto.caseAuditTrail;

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
public class CaseAuditTrailResponseDto {
    private UUID id;
    private UUID caseId;
    private UUID actorId;
    private String eventType;
    private String eventMetadata;
    private String ipAddress;
    private Instant sysCreatedAt;
}