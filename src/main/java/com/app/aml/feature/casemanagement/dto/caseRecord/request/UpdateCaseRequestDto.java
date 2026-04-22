package com.app.aml.feature.casemanagement.dto.caseRecord.request;

import com.app.aml.domain.enums.CasePriority;
import com.app.aml.domain.enums.CaseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCaseRequestDto {
    private UUID assignedTo;
    private CaseStatus status;
    private CasePriority priority;
    private Integer aggregatedRiskScore;
}