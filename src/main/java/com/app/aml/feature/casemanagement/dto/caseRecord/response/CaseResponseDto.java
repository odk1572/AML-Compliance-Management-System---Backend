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
    private String status;
    private String priority;
    private Integer aggregatedRiskScore;
    private UUID assignedTo;
    private Instant openedAt;
}