package com.app.aml.feature.casemanagement.service;


import com.app.aml.enums.AlertStatus;
import com.app.aml.enums.CaseStatus;
import com.app.aml.enums.ClosureDisposition;
import com.app.aml.feature.casemanagement.entity.CaseAlertLink;
import com.app.aml.feature.casemanagement.entity.CaseAuditTrail;
import com.app.aml.feature.casemanagement.entity.CaseRecord;
import com.app.aml.feature.casemanagement.repository.CaseAlertLinkRepository;
import com.app.aml.feature.casemanagement.repository.CaseAuditTrailRepository;
import com.app.aml.feature.casemanagement.repository.CaseRecordRepository;
import com.app.aml.feature.alert.entity.Alert;
import com.app.aml.feature.alert.repository.AlertRepository;
import com.app.aml.annotation.AuditAction;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CaseClosureServiceImpl implements CaseClosureService {

    private final CaseRecordRepository caseRepo;
    private final CaseAlertLinkRepository linkRepo;
    private final AlertRepository alertRepo;
    private final CaseAuditTrailRepository trailRepo;

    @Override
    @Transactional
    @AuditAction(category = "CASE_MGMT", action = "CLOSE_FALSE_POSITIVE", entityType = "CASE")
    public void closeAsFalsePositive(UUID caseId, String rationale, UUID closedBy, String ip) {
        if (rationale == null || rationale.trim().isEmpty()) {
            throw new IllegalArgumentException("Rationale cannot be blank for False Positive closure");
        }

        CaseRecord caseRecord = caseRepo.findById(caseId)
                .orElseThrow(() -> new EntityNotFoundException("Case not found"));

        caseRecord.setStatus(CaseStatus.CLOSED_NO_ACTION);
        caseRecord.setClosureDisposition(ClosureDisposition.FALSE_POSITIVE);
        caseRecord.setFalsePositiveRationale(rationale);
        caseRecord.setClosedAt(Instant.now());
        caseRecord.setClosedBy(closedBy);
        caseRecord.setLastActivityAt(Instant.now());
        caseRepo.save(caseRecord);

        List<CaseAlertLink> links = linkRepo.findByCaseRecordIdWithAlerts(caseId);
        List<Alert> linkedAlerts = links.stream().map(CaseAlertLink::getAlert).toList();

        linkedAlerts.forEach(alert -> alert.setStatus(AlertStatus.CLOSED_FALSE_POSITIVE));
        alertRepo.saveAll(linkedAlerts);

        CaseAuditTrail trail = new CaseAuditTrail();
        trail.setCaseRecord(caseRecord);
        trail.setActorId(closedBy);
        trail.setIpAddress(ip);
        trail.setEventType("CLOSED");
        trail.setEventMetadata("{\"disposition\": \"FALSE_POSITIVE\", \"rationale\": \"" + rationale.replace("\"", "\\\"") + "\"}");
        trailRepo.save(trail);
    }

    @Override
    @Transactional
    @AuditAction(category = "COMPLIANCE", action = "CLOSE_STR_FILED", entityType = "CASE")
    public void closeAfterStr(UUID caseId, UUID filingId, UUID closedBy, String ip) {
        CaseRecord caseRecord = caseRepo.findById(caseId)
                .orElseThrow(() -> new EntityNotFoundException("Case not found"));

        caseRecord.setStatus(CaseStatus.CLOSED_STR);
        caseRecord.setClosureDisposition(ClosureDisposition.STR_FILED);
        caseRecord.setClosedAt(Instant.now());
        caseRecord.setClosedBy(closedBy);
        caseRecord.setLastActivityAt(Instant.now());
        caseRepo.save(caseRecord);

        List<CaseAlertLink> links = linkRepo.findByCaseRecordIdWithAlerts(caseId);
        List<Alert> linkedAlerts = links.stream().map(CaseAlertLink::getAlert).toList();

        linkedAlerts.forEach(alert -> alert.setStatus(AlertStatus.CLOSED_CONFIRMED));
        alertRepo.saveAll(linkedAlerts);

        CaseAuditTrail trail = new CaseAuditTrail();
        trail.setCaseRecord(caseRecord);
        trail.setActorId(closedBy);
        trail.setIpAddress(ip);
        trail.setEventType("CLOSED");
        trail.setEventMetadata("{\"disposition\": \"STR_FILED\", \"strFilingId\": \"" + filingId + "\"}");
        trailRepo.save(trail);
    }
}