package com.app.aml.feature.casemanagement.service;


import com.app.aml.domain.enums.CaseStatus;
import com.app.aml.domain.enums.EscalationStatus;
import com.app.aml.feature.casemanagement.dto.request.EscalationRequestDto;
import com.app.aml.feature.casemanagement.entity.CaseAuditTrail;
import com.app.aml.feature.casemanagement.entity.CaseEscalation;
import com.app.aml.feature.casemanagement.entity.CaseRecord;
import com.app.aml.feature.casemanagement.repository.CaseAuditTrailRepository;
import com.app.aml.feature.casemanagement.repository.CaseEscalationRepository;
import com.app.aml.feature.casemanagement.repository.CaseRecordRepository;
import com.app.aml.feature.casemanagement.service.CaseEscalationService;
import com.app.aml.feature.notification.event.CaseEscalatedEvent;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CaseEscalationServiceImpl implements CaseEscalationService {

    private final CaseEscalationRepository escalationRepo;
    private final CaseRecordRepository caseRepo;
    private final CaseAuditTrailRepository trailRepo;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void escalate(UUID caseId, EscalationRequestDto dto, UUID escalatedById, String ip) {
        CaseRecord caseRecord = caseRepo.findById(caseId)
                .orElseThrow(() -> new EntityNotFoundException("Case not found"));

        caseRecord.setStatus(CaseStatus.ESCALATED);
        caseRecord.setLastActivityAt(Instant.now());
        caseRepo.save(caseRecord);

        CaseEscalation escalation = new CaseEscalation();
        escalation.setCaseRecord(caseRecord);
        escalation.setEscalatedBy(escalatedById);
        escalation.setEscalatedTo(dto.getEscalatedTo());
        escalation.setEscalationReason(dto.getEscalationReason());
        escalation.setEscalationStatus(EscalationStatus.PENDING);
        escalationRepo.save(escalation);

        CaseAuditTrail trail = new CaseAuditTrail();
        trail.setCaseRecord(caseRecord);
        trail.setActorId(escalatedById);
        trail.setIpAddress(ip);
        trail.setEventType("ESCALATED");
        trail.setEventMetadata("{\"escalatedTo\": \"" + dto.getEscalatedTo() + "\", \"reason\": \"" + dto.getEscalationReason() + "\"}");
        trailRepo.save(trail);

        String adminEmail = resolveUserEmail(dto.getEscalatedTo());
        eventPublisher.publishEvent(new CaseEscalatedEvent(this, caseRecord.getCaseReference(), adminEmail, dto.getEscalationReason()));
    }

    private String resolveUserEmail(UUID userId) {
        // Replace with actual user service/repository lookup in production
        return "admin_" + userId.toString().substring(0, 8) + "@bank.com";
    }
}