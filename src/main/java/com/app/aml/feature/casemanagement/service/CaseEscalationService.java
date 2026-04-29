package com.app.aml.feature.casemanagement.service;



import com.app.aml.feature.casemanagement.dto.caseEscalation.request.EscalationRequestDto;

import java.util.UUID;

public interface CaseEscalationService {
    void escalate(String caseRef, EscalationRequestDto dto, UUID escalatedById, String ip);
}