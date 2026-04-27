package com.app.aml.feature.casemanagement.service;

import com.app.aml.enums.CasePriority;
import com.app.aml.enums.CaseStatus;
// Assuming you have an AlertStatus enum based on your earlier alert assignment code

import com.app.aml.enums.AlertStatus;
import com.app.aml.feature.alert.entity.AlertTransaction;
import com.app.aml.feature.alert.repository.AlertTransactionRepository;
import com.app.aml.feature.casemanagement.dto.caseRecord.response.CaseResponseDto;
import com.app.aml.feature.casemanagement.entity.CaseAlertLink;
import com.app.aml.feature.casemanagement.entity.CaseAssignment;
import com.app.aml.feature.casemanagement.entity.CaseAuditTrail;
import com.app.aml.feature.casemanagement.entity.CaseRecord;
import com.app.aml.feature.casemanagement.repository.CaseAlertLinkRepository;
import com.app.aml.feature.casemanagement.repository.CaseAssignmentRepository;
import com.app.aml.feature.casemanagement.repository.CaseAuditTrailRepository;
import com.app.aml.feature.casemanagement.repository.CaseRecordRepository;
import com.app.aml.feature.alert.entity.Alert;
import com.app.aml.feature.alert.repository.AlertRepository;
import com.app.aml.feature.ingestion.entity.Transaction;
import com.app.aml.feature.notification.event.CaseAssignedEvent;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
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
    private final AlertTransactionRepository alertTxnRepo; // Injected
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

        List<Transaction> transactionsToMigrate = alerts.stream()
                .flatMap(alert -> alert.getAlertTransactions().stream())
                .map(AlertTransaction::getTransaction)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        transactionsToMigrate.forEach(caseRecord::addTransaction);

        CaseRecord savedCase = caseRepo.save(caseRecord);

        boolean isFirst = true;
        for (Alert alert : alerts) {
            CaseAlertLink link = new CaseAlertLink();
            link.setCaseRecord(savedCase);
            link.setAlert(alert);
            link.setLinkedBy(assignedById);
            link.setPrimaryAlert(isFirst);
            linkRepo.save(link);
            isFirst = false;

            alert.setStatus(AlertStatus.BUNDLED_TO_CASE);
        }

        alertRepo.saveAll(alerts);
        saveInitialAssignment(savedCase, assigneeId, assignedById);

        // Pass the correct transaction count to the audit trail
        saveAuditTrails(savedCase, assignedById, assigneeId, alerts.size(), transactionsToMigrate.size());

        String assigneeEmail = resolveUserEmail(assigneeId);
        eventPublisher.publishEvent(new CaseAssignedEvent(this, savedCase.getCaseReference(), assigneeEmail));

        return buildResponse(savedCase);
    }

    private void saveInitialAssignment(CaseRecord caseRecord, UUID assigneeId, UUID assignedById) {
        CaseAssignment assignment = new CaseAssignment();
        assignment.setCaseRecord(caseRecord);
        assignment.setAssignedTo(assigneeId);
        assignment.setAssignedBy(assignedById);
        assignment.setAssignmentReason("Initial case creation and assignment");
        assignmentRepo.save(assignment);
    }

    private void saveAuditTrails(CaseRecord caseRecord, UUID actorId, UUID assigneeId, int alertCount, int txnCount) {
        CaseAuditTrail createdTrail = new CaseAuditTrail();
        createdTrail.setCaseRecord(caseRecord);
        createdTrail.setActorId(actorId);
        createdTrail.setEventType("CREATED");
        createdTrail.setEventMetadata(String.format("{\"alertsLinked\": %d, \"txnsLinked\": %d}", alertCount, txnCount));
        trailRepo.save(createdTrail);

        CaseAuditTrail assignedTrail = new CaseAuditTrail();
        assignedTrail.setCaseRecord(caseRecord);
        assignedTrail.setActorId(actorId);
        assignedTrail.setEventType("ASSIGNED");
        assignedTrail.setEventMetadata("{\"assignedTo\": \"" + assigneeId + "\"}");
        trailRepo.save(assignedTrail);
    }

    private CaseResponseDto buildResponse(CaseRecord savedCase) {

        List<CaseResponseDto.LinkedTransactionDto> transactionDtos = savedCase.getCaseTransactions().stream()
                .map(ct -> {
                    var txn = ct.getTransaction();
                    return CaseResponseDto.LinkedTransactionDto.builder()
                            .id(txn.getId())
                            .transactionReference(txn.getTransactionRef())
                            .amount(txn.getAmount())
                            .currency(txn.getCurrencyCode())
                            .timestamp(txn.getTransactionTimestamp())
                            .originatorAccount(txn.getOriginatorAccountNo())
                            .beneficiaryAccount(txn.getBeneficiaryAccountNo())
                            .build();
                })
                .toList();

        return CaseResponseDto.builder()
                .id(savedCase.getId())
                .caseReference(savedCase.getCaseReference())
                .status(savedCase.getStatus().name())
                .priority(savedCase.getPriority().name())
                .aggregatedRiskScore(savedCase.getAggregatedRiskScore())
                .assignedTo(savedCase.getAssignedTo())
                .openedAt(savedCase.getOpenedAt())
                .transactions(transactionDtos)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CaseResponseDto getCaseDetails(UUID caseId) {
        log.info("Fetching details for Case ID: {}", caseId);

        CaseRecord caseRecord = caseRepo.findById(caseId)
                .orElseThrow(() -> new EntityNotFoundException("Case not found with ID: " + caseId));

        return buildResponse(caseRecord);
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