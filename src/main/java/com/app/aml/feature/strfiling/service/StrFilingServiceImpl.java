package com.app.aml.feature.strfiling.service;

import com.app.aml.exceptions.StrFilingGateException;
import com.app.aml.feature.casemanagement.entity.CaseAuditTrail;
import com.app.aml.feature.casemanagement.entity.CaseNote;
import com.app.aml.feature.casemanagement.entity.CaseRecord;
import com.app.aml.feature.casemanagement.entity.CaseTransaction;
import com.app.aml.feature.casemanagement.repository.CaseAuditTrailRepository;
import com.app.aml.feature.casemanagement.repository.CaseNoteRepository;
import com.app.aml.feature.casemanagement.repository.CaseRecordRepository;
import com.app.aml.feature.casemanagement.service.CaseClosureService;
import com.app.aml.feature.ingestion.entity.Transaction;
import com.app.aml.feature.notification.event.StrFiledEvent;
import com.app.aml.feature.strfiling.dto.strFiling.StrFilingRequestDto;
import com.app.aml.feature.strfiling.dto.strFiling.StrFilingResponseDto;
import com.app.aml.feature.strfiling.entity.StrFiling;
import com.app.aml.feature.strfiling.entity.StrFilingTransaction;
import com.app.aml.feature.strfiling.mapper.StrFilingMapper;
import com.app.aml.feature.strfiling.repository.StrFilingRepository;
import com.app.aml.feature.strfiling.repository.StrFilingTransactionRepository;
import com.app.aml.multitenency.TenantContext;
import com.app.aml.annotation.AuditAction;
import com.app.aml.cloudinary.CloudinaryService;
import com.app.aml.utils.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StrFilingServiceImpl implements StrFilingService {

    private final StrFilingRepository strRepo;
    private final StrFilingTransactionRepository stRepo;
    private final CaseRecordRepository caseRepo;
    private final CaseNoteRepository noteRepo;
    private final CaseAuditTrailRepository trailRepo;
    private final StrDocumentGenerator docGen;
    private final CloudinaryService cloudinary;
    private final CaseClosureService closureService;
    private final ApplicationEventPublisher eventPublisher;
    private final StrFilingMapper strFilingMapper;

    @Override
    @Transactional
    @AuditAction(category = "COMPLIANCE", action = "FILE_STR", entityType = "STR")
    public StrFilingResponseDto fileSar(UUID caseId, StrFilingRequestDto dto, UUID filedBy, String ip) {
        ensureTenantContext();
        validateGate(caseId);

        CaseRecord caseRecord = caseRepo.findById(caseId)
                .orElseThrow(() -> new EntityNotFoundException("Case not found"));

        List<Transaction> transactionsToLink = caseRecord.getCaseTransactions().stream()
                .map(CaseTransaction::getTransaction)
                .toList();

        if (transactionsToLink.isEmpty()) {
            throw new IllegalArgumentException("Cannot file STR: No transactions linked to Case.");
        }

        StrFiling filing = new StrFiling();
        filing.setCaseRecord(caseRecord);
        filing.setFilingReference("STR-" + Instant.now().toEpochMilli());
        filing.setRegulatoryBody(dto.getRegulatoryBody());

        filing.setRuleType(caseRecord.getRuleType());
        filing.setTypologyTriggered(caseRecord.getTypologyTriggered());

        filing.setSuspicionNarrative(dto.getSuspicionNarrative());
        filing.setFiledBy(filedBy);
        filing.setCustomer(caseRecord.getCustomer());

        transactionsToLink.forEach(filing::addTransaction);

        StrFiling savedFiling = strRepo.save(filing);

        List<CaseNote> notes = noteRepo.findByCaseRecordIdOrderBySysCreatedAtDesc(caseId);
        List<String> evidence = transactionsToLink.stream()
                .map(t -> "Txn ID: " + t.getId() + " | Ref: " + t.getTransactionRef() + " | Amount: " + t.getAmount())
                .toList();

        byte[] pdfBytes = docGen.generatePdf(savedFiling, transactionsToLink, notes, evidence);
        cloudinary.uploadRawBytes(pdfBytes, savedFiling.getFilingReference() + ".pdf", "str_reports");

        if ("FIU_IND".equalsIgnoreCase(dto.getRegulatoryBody())) {
            byte[] xmlBytes = docGen.generateXml(savedFiling, transactionsToLink);
            cloudinary.uploadRawBytes(xmlBytes, savedFiling.getFilingReference() + ".xml", "str_reports_xml");
        }

        closureService.closeAfterStr(caseId, savedFiling.getId(), filedBy, ip);

        CaseAuditTrail trail = new CaseAuditTrail();
        trail.setCaseRecord(caseRecord);
        trail.setActorId(filedBy);
        trail.setEventType("STR_FILED");
        trail.setEventMetadata(String.format("{\"strFilingId\": \"%s\", \"autoEnriched\": true}", savedFiling.getId()));
        trailRepo.save(trail);

        eventPublisher.publishEvent(new StrFiledEvent(this, savedFiling.getFilingReference(), resolveUserEmail(filedBy)));

        return strFilingMapper.toResponseDto(savedFiling);
    }

    @Override
    @Transactional(readOnly = true)
    @AuditAction(category = "DATA_ACCESS", action = "VIEW_STR_DETAILS", entityType = "STR")
    public StrFilingResponseDto getFilingDetail(UUID filingId) {
        ensureTenantContext();
        StrFiling filing = strRepo.findById(filingId)
                .orElseThrow(() -> new EntityNotFoundException("STR Filing not found"));

        return strFilingMapper.toResponseDto(filing);
    }

    @Override
    @Transactional(readOnly = true)
    @AuditAction(category = "DATA_ACCESS", action = "EXPORT_STR_PDF", entityType = "STR")
    public byte[] getPdfReport(UUID filingId) {
        ensureTenantContext();

        StrFiling filing = strRepo.findById(filingId)
                .orElseThrow(() -> new EntityNotFoundException("STR Filing not found"));

        List<Transaction> transactions = filing.getStrTransactions().stream()
                .map(StrFilingTransaction::getTransaction)
                .toList();

        List<CaseNote> notes = noteRepo.findByCaseRecordIdOrderBySysCreatedAtDesc(filing.getCaseRecord().getId());
        List<String> evidence = transactions.stream()
                .map(t -> "Txn ID: " + t.getId() + " | Amount: " + t.getAmount())
                .toList();

        return docGen.generatePdf(filing, transactions, notes, evidence);
    }

    @Override
    @Transactional(readOnly = true)
    @AuditAction(category = "COMPLIANCE", action = "VALIDATE_STR_FILING_GATE", entityType = "STR")
    public void validateGate(UUID caseId) {
        ensureTenantContext();
        CaseRecord caseRecord = caseRepo.findById(caseId)
                .orElseThrow(() -> new EntityNotFoundException("Case not found"));

        if (!caseRecord.isHasInvestigationNote()) {
            throw new StrFilingGateException("Cannot file STR: Case must have at least one investigation note.");
        }
    }

    private String resolveUserEmail(UUID userId) {
        return SecurityUtils.getCurrentUserEmail();
    }

    private void ensureTenantContext() {
        if (TenantContext.getTenantId() == null) {
            log.error("Multi-tenancy violation: No Tenant ID found in current context.");
            throw new IllegalStateException("Tenant context is missing. Request cannot be processed.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    @AuditAction(category = "DATA_ACCESS", action = "VIEW_STR_BY_CASE", entityType = "STR")
    public StrFilingResponseDto getFilingByCaseId(UUID caseId) {
        ensureTenantContext();
        StrFiling filing = strRepo.findByCaseRecordId(caseId)
                .orElseThrow(() -> new EntityNotFoundException("STR Filing not found for Case ID: " + caseId));
        return strFilingMapper.toResponseDto(filing);
    }

    @Override
    @Transactional(readOnly = true)
    @AuditAction(category = "DATA_ACCESS", action = "LIST_STR_FILINGS", entityType = "STR")
    public List<StrFilingResponseDto> getAllFilings() {
        ensureTenantContext();
        return strRepo.findAll().stream()
                .map(strFilingMapper::toResponseDto)
                .collect(Collectors.toList());
    }

}