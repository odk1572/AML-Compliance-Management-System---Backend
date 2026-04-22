package com.app.aml.feature.casemanagement.dto.caseRecord.response;

import com.app.aml.domain.enums.CasePriority;
import com.app.aml.domain.enums.CaseStatus;
import com.app.aml.domain.enums.ClosureDisposition;
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
public class CaseResponseDto {
    private UUID id;
    private String caseReference;
    private UUID assignedTo;
    private UUID assignedBy;
    private CaseStatus status;
    private CasePriority priority;
    private Integer aggregatedRiskScore;
    private Instant openedAt;
    private Instant lastActivityAt;
    private Instant closedAt;
    private UUID closedBy;
    private ClosureDisposition closureDisposition;
    private String falsePositiveRationale;
    private boolean hasInvestigationNote;
    private Instant sysCreatedAt;
    private Instant sysUpdatedAt;
}