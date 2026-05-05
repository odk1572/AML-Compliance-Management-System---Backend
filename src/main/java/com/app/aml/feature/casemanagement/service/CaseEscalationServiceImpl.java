package com.app.aml.feature.casemanagement.service;


import com.app.aml.enums.CaseStatus;
import com.app.aml.enums.EscalationStatus;
import com.app.aml.feature.casemanagement.dto.caseEscalation.request.EscalationRequestDto;
import com.app.aml.feature.casemanagement.entity.CaseAuditTrail;
import com.app.aml.feature.casemanagement.entity.CaseEscalation;
import com.app.aml.feature.casemanagement.entity.CaseRecord;
import com.app.aml.feature.casemanagement.repository.CaseAuditTrailRepository;
import com.app.aml.feature.casemanagement.repository.CaseEscalationRepository;
import com.app.aml.feature.casemanagement.repository.CaseRecordRepository;
import com.app.aml.feature.notification.event.CaseEscalatedEvent;
import com.app.aml.annotation.AuditAction;
import com.app.aml.feature.platformuser.entity.PlatformUser;
import com.app.aml.feature.tenantuser.entity.TenantUser;
import com.app.aml.feature.tenantuser.repository.TenantUserRepository;
import com.app.aml.utils.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CaseEscalationServiceImpl implements CaseEscalationService {

    private final CaseEscalationRepository escalationRepo;
    private final CaseRecordRepository caseRepo;
    private final CaseAuditTrailRepository trailRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final TenantUserRepository userRepository;
    private final ObjectMapper objectMapper;


    @Override
    @Transactional
    @AuditAction(category = "CASE_MGMT", action = "ESCALATE_CASE", entityType = "CASE")
    public void escalate(String caseRef, EscalationRequestDto dto, UUID escalatedById, String ip) {
        CaseRecord caseRecord = caseRepo.findByCaseReference(caseRef)
                .orElseThrow(() -> new EntityNotFoundException("Case not found"));

        caseRecord.setStatus(CaseStatus.ESCALATED);
        caseRecord.setLastActivityAt(Instant.now());
        caseRecord.setAssignedTo(dto.getEscalatedTo());
        caseRecord.setAssignedBy(escalatedById);
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
        Map<String, Object> metaMap = Map.of(
                "escalatedTo", dto.getEscalatedTo(),
                "reason", dto.getEscalationReason()
        );
        try {
            trail.setEventMetadata(objectMapper.writeValueAsString(metaMap));
        } catch (JsonProcessingException e) {
            trail.setEventMetadata("{}");
        }
        trailRepo.save(trail);

        String adminEmail = resolveUserEmail(dto.getEscalatedTo());
        eventPublisher.publishEvent(new CaseEscalatedEvent(SecurityUtils.getCurrentUserEmail(), caseRecord.getCaseReference(), adminEmail, dto.getEscalationReason()));
    }

    private String resolveUserEmail(UUID userId) {
        return userRepository.findById(userId)
                .map(TenantUser::getEmail)
                .orElseThrow(() -> new EntityNotFoundException("Assignee email not found"));
    }

}