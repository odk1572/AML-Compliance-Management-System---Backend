package com.app.aml.feature.casemanagement.service;

import com.app.aml.domain.enums.CasePriority;
import com.app.aml.domain.enums.CaseStatus;
// Assuming you have an AlertStatus enum based on your earlier alert assignment code

import com.app.aml.domain.enums.AlertStatus;
import com.app.aml.feature.casemanagement.dto.caseRecord.response.CaseResponseDto;
import com.app.aml.feature.casemanagement.entity.CaseAlertLink;
import com.app.aml.feature.casemanagement.entity.CaseAssignment;
import com.app.aml.feature.casemanagement.entity.CaseAuditTrail;
import com.app.aml.feature.casemanagement.entity.CaseRecord;
import com.app.aml.feature.casemanagement.repository.CaseAlertLinkRepository;
import com.app.aml.feature.casemanagement.repository.CaseAssignmentRepository;
import com.app.aml.feature.casemanagement.repository.CaseAuditTrailRepository;
import com.app.aml.feature.casemanagement.repository.CaseRecordRepository;
import com.app.aml.feature.ingestion.entity.Alert;
import com.app.aml.feature.ingestion.repository.AlertRepository;
import com.app.aml.feature.notification.event.CaseAssignedEvent;
import com.app.aml.multitenency.TenantContext;
import com.app.aml.security.userDetails.TenantUserDetails;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CaseAssignmentServiceImpl implements CaseAssignmentService {

    private final CaseRecordRepository caseRepo;
    private final CaseAlertLinkRepository linkRepo;
    private final CaseAssignmentRepository assignmentRepo;
    private final CaseAuditTrailRepository trailRepo;
    private final AlertRepository alertRepo;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public CaseResponseDto createCase(List<UUID> alertIds, UUID assigneeId, UUID assignedById, String priority) {

        List<Alert> alerts = alertRepo.findAllById(alertIds);
        if (alerts.isEmpty()) {
            throw new IllegalArgumentException("No valid alerts provided");
        }

        int totalRisk = alerts.stream()
                .map(alert -> alert.getRiskScore() != null ? alert.getRiskScore().intValue() : 0)
                .mapToInt(Integer::intValue)
                .sum();

        CaseRecord caseRecord = new CaseRecord();
        caseRecord.setCaseReference("CAS-" + Instant.now().toEpochMilli());
        caseRecord.setAssignedTo(assigneeId);
        caseRecord.setAssignedBy(assignedById);
        caseRecord.setStatus(CaseStatus.OPEN);
        caseRecord.setPriority(CasePriority.valueOf(priority.toUpperCase()));
        caseRecord.setAggregatedRiskScore(totalRisk);
        caseRecord.setOpenedAt(Instant.now());
        caseRecord.setLastActivityAt(Instant.now());

        CaseRecord savedCase = caseRepo.save(caseRecord);

        boolean isFirst = true;
        for (Alert alert : alerts) {
            // 1. Create the link
            CaseAlertLink link = new CaseAlertLink();
            link.setCaseRecord(savedCase);
            link.setAlert(alert);
            link.setLinkedBy(assignedById);
            link.setPrimaryAlert(isFirst);
            linkRepo.save(link);
            isFirst = false;

            // 2. UPDATE ALERT STATUS (Crucial AML Lifecycle step)
            // The alert is no longer 'NEW', it is now part of an active case
            alert.setStatus(AlertStatus.BUNDLED_TO_CASE); // Or whatever your enum value is
        }

        // Save the updated alert statuses to the database
        alertRepo.saveAll(alerts);

        CaseAssignment assignment = new CaseAssignment();
        assignment.setCaseRecord(savedCase);
        assignment.setAssignedFrom(null);
        assignment.setAssignedTo(assigneeId);
        assignment.setAssignedBy(assignedById);
        assignment.setAssignmentReason("Initial case creation and assignment");
        assignmentRepo.save(assignment);

        CaseAuditTrail createdTrail = new CaseAuditTrail();
        createdTrail.setCaseRecord(savedCase);
        createdTrail.setActorId(assignedById);
        createdTrail.setEventType("CREATED");
        createdTrail.setEventMetadata("{\"alertsLinked\": " + alerts.size() + "}");
        trailRepo.save(createdTrail);

        CaseAuditTrail assignedTrail = new CaseAuditTrail();
        assignedTrail.setCaseRecord(savedCase);
        assignedTrail.setActorId(assignedById);
        assignedTrail.setEventType("ASSIGNED");
        assignedTrail.setEventMetadata("{\"assignedTo\": \"" + assigneeId + "\"}");
        trailRepo.save(assignedTrail);

        String assigneeEmail = resolveUserEmail(assigneeId);
        eventPublisher.publishEvent(new CaseAssignedEvent(this, savedCase.getCaseReference(), assigneeEmail));

        return CaseResponseDto.builder()
                .id(savedCase.getId())
                .caseReference(savedCase.getCaseReference())
                .status(savedCase.getStatus().name())
                .priority(savedCase.getPriority().name())
                .aggregatedRiskScore(savedCase.getAggregatedRiskScore())
                .assignedTo(savedCase.getAssignedTo())
                .openedAt(savedCase.getOpenedAt())
                .build();
    }

    @Override
    @Transactional
    public void reassignCase(UUID caseId, UUID newAssigneeId, UUID reassignedById, String reason) {
        CaseRecord caseRecord = caseRepo.findById(caseId)
                .orElseThrow(() -> new EntityNotFoundException("Case not found"));

        UUID oldAssigneeId = caseRecord.getAssignedTo();

        caseRecord.setAssignedTo(newAssigneeId);
        caseRecord.setLastActivityAt(Instant.now());
        caseRepo.save(caseRecord);

        CaseAssignment assignment = new CaseAssignment();
        assignment.setCaseRecord(caseRecord);
        assignment.setAssignedFrom(oldAssigneeId);
        assignment.setAssignedTo(newAssigneeId);
        assignment.setAssignedBy(reassignedById);
        assignment.setAssignmentReason(reason);
        assignmentRepo.save(assignment);

        CaseAuditTrail reassignedTrail = new CaseAuditTrail();
        reassignedTrail.setCaseRecord(caseRecord);
        reassignedTrail.setActorId(reassignedById);
        reassignedTrail.setEventType("REASSIGNED");
        reassignedTrail.setEventMetadata("{\"assignedFrom\": \"" + oldAssigneeId + "\", \"assignedTo\": \"" + newAssigneeId + "\", \"reason\": \"" + reason + "\"}");
        trailRepo.save(reassignedTrail);

        String newAssigneeEmail = resolveUserEmail(newAssigneeId);
        eventPublisher.publishEvent(new CaseAssignedEvent(this, caseRecord.getCaseReference(), newAssigneeEmail));
    }

    private String resolveUserEmail(UUID userId) {
        return "investigator_" + userId.toString().substring(0, 8) + "@bank.com";
    }
}