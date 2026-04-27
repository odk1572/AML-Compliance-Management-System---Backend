package com.app.aml.feature.casemanagement.service;


import com.app.aml.feature.casemanagement.dto.caseAuditTrail.CaseAuditTrailResponseDto;
import com.app.aml.feature.casemanagement.dto.request.CaseNoteRequestDto;
import com.app.aml.feature.alert.dto.alert.response.AlertResponseDto;

import java.util.List;
import java.util.UUID;

public interface CaseInvestigationService {
    void openCase(UUID caseId, UUID actorId, String ip);
    void addNote(UUID caseId, CaseNoteRequestDto dto, UUID authoredBy, String ip);
    List<CaseAuditTrailResponseDto> getCaseAuditTrail(UUID caseId);
    byte[] exportAuditTrailAsPdf(UUID caseId);
    List<AlertResponseDto> getAlertsForCase(UUID caseId);
}