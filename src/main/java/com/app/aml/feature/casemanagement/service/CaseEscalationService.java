package com.app.aml.feature.casemanagement.service;


import com.app.aml.feature.casemanagement.dto.request.EscalationRequestDto;

import java.util.UUID;

public interface CaseEscalationService {
    void escalate(UUID caseId, EscalationRequestDto dto, UUID escalatedById, String ip);
}