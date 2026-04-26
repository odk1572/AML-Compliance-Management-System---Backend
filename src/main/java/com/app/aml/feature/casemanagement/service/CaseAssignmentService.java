package com.app.aml.feature.casemanagement.service;

import com.app.aml.feature.casemanagement.dto.caseRecord.response.CaseResponseDto;

import java.util.List;
import java.util.UUID;

public interface CaseAssignmentService {
    CaseResponseDto createCase(List<UUID> alertIds, UUID assigneeId, UUID assignedById, String priority);
    void reassignCase(UUID caseId, UUID newAssigneeId, UUID reassignedById, String reason);
}
