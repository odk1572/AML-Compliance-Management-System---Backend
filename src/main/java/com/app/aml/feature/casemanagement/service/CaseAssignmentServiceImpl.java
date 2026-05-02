package com.app.aml.feature.casemanagement.service;

import com.app.aml.UX.ReferenceGenerator;
import com.app.aml.enums.CasePriority;
import com.app.aml.enums.CaseStatus;
import com.app.aml.enums.AlertStatus;
import com.app.aml.feature.alert.entity.Alert;
import com.app.aml.feature.alert.entity.AlertTransaction;
import com.app.aml.feature.alert.repository.AlertRepository;
import com.app.aml.feature.alert.repository.AlertTransactionRepository;
import com.app.aml.feature.casemanagement.dto.CreateCaseRequest;
import com.app.aml.feature.casemanagement.dto.ReassignCaseRequest;
import com.app.aml.feature.casemanagement.dto.caseRecord.response.CaseResponseDto;
import com.app.aml.feature.casemanagement.entity.CaseAlertLink;
import com.app.aml.feature.casemanagement.entity.CaseAssignment;
import com.app.aml.feature.casemanagement.entity.CaseAuditTrail;
import com.app.aml.feature.casemanagement.entity.CaseRecord;
import com.app.aml.feature.casemanagement.mapper.CaseRecordMapper;
import com.app.aml.feature.casemanagement.repository.CaseAlertLinkRepository;
import com.app.aml.feature.casemanagement.repository.CaseAssignmentRepository;
import com.app.aml.feature.casemanagement.repository.CaseAuditTrailRepository;
import com.app.aml.feature.casemanagement.repository.CaseRecordRepository;
import com.app.aml.feature.ingestion.entity.Transaction;
import com.app.aml.feature.notification.event.CaseAssignedEvent;
import com.app.aml.feature.tenantuser.entity.TenantUser;
import com.app.aml.feature.tenantuser.repository.TenantUserRepository;

import com.app.aml.annotation.AuditAction;
import com.app.aml.utils.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final AlertTransactionRepository alertTxnRepo;
    private final TenantUserRepository tenantUserRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final CaseRecordMapper caseRecordMapper;


    @Override
    @Transactional
    @AuditAction(category = "CASE_MGMT", action = "CREATE_CASE", entityType = "CASE")
    public CaseResponseDto createCase(CreateCaseRequest request) {

        UUID actorId = SecurityUtils.getCurrentUserId();

        TenantUser assignee = tenantUserRepo.findByEmployeeId(request.getAssigneeUserCode())
                .orElseThrow(() -> new EntityNotFoundException("Assignee not found with Employee ID: " + request.getAssigneeUserCode()));

        List<Alert> alerts = alertRepo.findAllByAlertReferenceIn(request.getAlertReferences());
        if (alerts.isEmpty() || alerts.size() != request.getAlertReferences().size()) {
            throw new IllegalArgumentException("One or more alert references are invalid");
        }

        List<String> alreadyBundledAlerts = alerts.stream()
                .filter(alert -> alert.getStatus() == AlertStatus.BUNDLED_TO_CASE)
                .map(Alert::getAlertReference)
                .toList();

        if (!alreadyBundledAlerts.isEmpty()) {
            throw new IllegalStateException("Duplicate Operation: The following alerts are already bundled to a case: " + alreadyBundledAlerts);
        }

        long distinctCustomerCount = alerts.stream()
                .map(alert -> alert.getCustomer().getId())
                .distinct()
                .count();

        if (distinctCustomerCount > 1) {
            throw new IllegalArgumentException("Invalid operation: You cannot bundle alerts from different customers into a single case.");
        }

        Alert primaryAlertMetadata = alerts.get(0);

        List<Transaction> transactionsToMigrate = alerts.stream()
                .flatMap(alert -> alert.getAlertTransactions().stream())
                .map(AlertTransaction::getTransaction)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (!transactionsToMigrate.isEmpty()) {
            List<UUID> transactionIds = transactionsToMigrate.stream()
                    .map(Transaction::getId)
                    .toList();

            boolean duplicateTransactionExists = caseRepo.existsByCustomerAndStatusInAndCaseTransactions_Transaction_IdIn(
                    primaryAlertMetadata.getCustomer(),
                    List.of(CaseStatus.OPEN, CaseStatus.IN_PROGRESS),
                    transactionIds
            );

            if (duplicateTransactionExists) {
                throw new IllegalStateException("Duplicate Investigation: An active case already exists for this customer involving one or more of these specific transactions.");
            }
        }

        int totalRisk = alerts.stream()
                .map(alert -> alert.getRiskScore() != null ? alert.getRiskScore().intValue() : 0)
                .mapToInt(Integer::intValue)
                .sum();

        CaseRecord caseRecord = new CaseRecord();
        caseRecord.setAssignedTo(assignee.getId());
        caseRecord.setAssignedBy(actorId);
        caseRecord.setStatus(CaseStatus.OPEN);
        caseRecord.setCaseReference(ReferenceGenerator.generate("CAS"));
        caseRecord.setPriority(CasePriority.valueOf(request.getPriority().toUpperCase()));
        caseRecord.setAggregatedRiskScore(totalRisk);
        caseRecord.setOpenedAt(Instant.now());
        caseRecord.setLastActivityAt(Instant.now());
        caseRecord.setRuleType(primaryAlertMetadata.getRuleType());
        caseRecord.setTypologyTriggered(primaryAlertMetadata.getTypologyTriggered());
        caseRecord.setCustomer(primaryAlertMetadata.getCustomer());

        transactionsToMigrate.forEach(caseRecord::addTransaction);

        CaseRecord savedCase = caseRepo.save(caseRecord);

        boolean isFirst = true;
        for (Alert alert : alerts) {
            CaseAlertLink link = new CaseAlertLink();
            link.setCaseRecord(savedCase);
            link.setAlert(alert);
            link.setLinkedBy(actorId);
            link.setPrimaryAlert(isFirst);
            linkRepo.save(link);
            isFirst = false;

            alert.setStatus(AlertStatus.BUNDLED_TO_CASE);
        }

        alertRepo.saveAll(alerts);
        saveInitialAssignment(savedCase, assignee.getId(), actorId);

        saveAuditTrails(savedCase, actorId, assignee.getEmployeeId(), alerts.size(), transactionsToMigrate.size());

        eventPublisher.publishEvent(new CaseAssignedEvent(this, savedCase.getCaseReference(), assignee.getEmail()));

        return buildResponse(savedCase);
    }

    private CaseResponseDto buildResponse(CaseRecord savedCase) {
        return caseRecordMapper.toResponseDto(savedCase);
    }

    private void saveInitialAssignment(CaseRecord caseRecord, UUID assigneeId, UUID actorId) {
        CaseAssignment assignment = new CaseAssignment();
        assignment.setCaseRecord(caseRecord);
        assignment.setAssignedTo(assigneeId);
        assignment.setAssignedBy(actorId);
        assignment.setAssignmentReason("Initial case creation and assignment");
        assignmentRepo.save(assignment);
    }

    private void saveAuditTrails(CaseRecord caseRecord, UUID actorId, String assigneeEmployeeId, int alertCount, int txnCount) {
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
        assignedTrail.setEventMetadata("{\"assignedTo\": \"" + assigneeEmployeeId + "\"}");
        trailRepo.save(assignedTrail);
    }

    @Override
    @Transactional(readOnly = true)
    @AuditAction(category = "DATA_ACCESS", action = "VIEW_CASE_DETAILS", entityType = "CASE")
    public CaseResponseDto getCaseDetails(String caseReference) {
        log.info("Fetching details for Case Reference: {}", caseReference);

        CaseRecord caseRecord = caseRepo.findByCaseReference(caseReference)
                .orElseThrow(() -> new EntityNotFoundException("Case not found with reference: " + caseReference));

        return buildResponse(caseRecord);
    }

    @Override
    @Transactional
    @AuditAction(category = "CASE_MGMT", action = "REASSIGN_CASE", entityType = "CASE")
    public void reassignCase(String caseReference, ReassignCaseRequest request) {

        CaseRecord caseRecord = caseRepo.findByCaseReference(caseReference)
                .orElseThrow(() -> new EntityNotFoundException("Case not found: " + caseReference));

        if(caseRecord.getStatus()==CaseStatus.CLOSED_NO_ACTION || caseRecord.getStatus()==CaseStatus.CLOSED_STR ){
            throw new RuntimeException("Case is already closed");
        }

        UUID actorId = SecurityUtils.getCurrentUserId();
        TenantUser newAssignee = tenantUserRepo.findByEmployeeId(request.getNewAssigneeUserCode())
                .orElseThrow(() -> new EntityNotFoundException("Assignee not found with Employee ID: " + request.getNewAssigneeUserCode()));

        UUID oldAssigneeId = caseRecord.getAssignedTo();

        String oldAssigneeCode = tenantUserRepo.findById(oldAssigneeId)
                .map(TenantUser::getEmployeeId)
                .orElse("UNKNOWN");


        caseRecord.setAssignedTo(newAssignee.getId());
        caseRecord.setLastActivityAt(Instant.now());
        caseRepo.save(caseRecord);

        CaseAssignment assignment = new CaseAssignment();
        assignment.setCaseRecord(caseRecord);
        assignment.setAssignedFrom(oldAssigneeId);
        assignment.setAssignedTo(newAssignee.getId());
        assignment.setAssignedBy(actorId);
        assignment.setAssignmentReason(request.getReason());
        assignmentRepo.save(assignment);


        CaseAuditTrail reassignedTrail = new CaseAuditTrail();
        reassignedTrail.setCaseRecord(caseRecord);
        reassignedTrail.setActorId(actorId);
        reassignedTrail.setEventType("REASSIGNED");
        reassignedTrail.setEventMetadata("{\"assignedFrom\": \"" + oldAssigneeCode + "\", \"assignedTo\": \"" + newAssignee.getEmployeeId() + "\", \"reason\": \"" + request.getReason() + "\"}");
        trailRepo.save(reassignedTrail);

        eventPublisher.publishEvent(new CaseAssignedEvent(this, caseRecord.getCaseReference(), newAssignee.getEmail()));
    }
    @Override
    @Transactional(readOnly = true)
    @AuditAction(category = "DATA_ACCESS", action = "VIEW_ALL_CASES", entityType = "CASE")
    public Page<CaseResponseDto> getAllCases(Pageable pageable) {
        log.info("Fetching paginated list of all cases for investigation");
        return caseRepo.findAll(pageable)
                .map(caseRecordMapper::toResponseDto);
    }
}