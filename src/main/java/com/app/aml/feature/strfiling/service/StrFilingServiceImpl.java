package com.app.aml.feature.strfiling.service;

import com.app.aml.domain.exceptions.StrFilingGateException;
import com.app.aml.feature.casemanagement.entity.CaseAuditTrail;
import com.app.aml.feature.casemanagement.entity.CaseNote;
import com.app.aml.feature.casemanagement.entity.CaseRecord;
import com.app.aml.feature.casemanagement.repository.CaseAuditTrailRepository;
import com.app.aml.feature.casemanagement.repository.CaseNoteRepository;
import com.app.aml.feature.casemanagement.repository.CaseRecordRepository;
import com.app.aml.feature.casemanagement.service.CaseClosureService;
import com.app.aml.feature.ingestion.entity.Transaction;
import com.app.aml.feature.ingestion.repository.TransactionRepository;
import com.app.aml.feature.notification.event.StrFiledEvent;
import com.app.aml.feature.strfiling.dto.strFiling.StrFilingRequestDto;
import com.app.aml.feature.strfiling.dto.strFiling.StrFilingResponseDto;
import com.app.aml.feature.strfiling.entity.StrFiling;
import com.app.aml.feature.strfiling.entity.StrFilingTransaction;
import com.app.aml.feature.strfiling.repository.StrFilingRepository;
import com.app.aml.feature.strfiling.repository.StrFilingTransactionRepository;
import com.app.aml.multitenency.TenantContext;

import com.app.aml.shared.cloudinary.CloudinaryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StrFilingServiceImpl implements StrFilingService {

    private final StrFilingRepository strRepo;
    private final StrFilingTransactionRepository stRepo;
    private final CaseRecordRepository caseRepo;
    private final CaseNoteRepository noteRepo;
    private final TransactionRepository transactionRepo;
    private final CaseAuditTrailRepository trailRepo;
    private final StrDocumentGenerator docGen;
    private final CloudinaryService cloudinary;
    private final CaseClosureService closureService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public StrFilingResponseDto fileSar(UUID caseId, StrFilingRequestDto dto, UUID filedBy, String ip) {
        ensureTenantContext();
        validateGate(caseId);

        CaseRecord caseRecord = caseRepo.findById(caseId)
                .orElseThrow(() -> new EntityNotFoundException("Case not found"));

        StrFiling filing = new StrFiling();
        filing.setCaseRecord(caseRecord);
        filing.setFilingReference("STR-" + Instant.now().toEpochMilli());
        filing.setRegulatoryBody(dto.getRegulatoryBody());
        filing.setTypologyCategory(dto.getTypologyCategory());
        filing.setSubjectName(dto.getSubjectName());
        filing.setSubjectAccountNo(dto.getSubjectAccountNo());
        filing.setSuspicionNarrative(dto.getSuspicionNarrative());
        filing.setFiledBy(filedBy);

        StrFiling savedFiling = strRepo.save(filing);

        List<Transaction> transactions = transactionRepo.findAllById(dto.getTransactionIds());
        for (Transaction transaction : transactions) {
            StrFilingTransaction filingTransaction = new StrFilingTransaction();
            filingTransaction.setStrFiling(savedFiling);
            filingTransaction.setTransaction(transaction);
            stRepo.save(filingTransaction);
        }

        List<CaseNote> notes = noteRepo.findByCaseRecordIdOrderBySysCreatedAtDesc(caseId);
        List<String> evidence = transactions.stream()
                .map(t -> "Txn ID: " + t.getId() + " | Amount: " + t.getAmount())
                .toList();

        byte[] pdfBytes = docGen.generatePdf(savedFiling, transactions, notes, evidence);
        cloudinary.uploadRawBytes(pdfBytes, savedFiling.getFilingReference() + ".pdf", "str_reports");

        if ("FIU_IND".equalsIgnoreCase(dto.getRegulatoryBody())) {
            byte[] xmlBytes = docGen.generateXml(savedFiling, transactions);
            cloudinary.uploadRawBytes(xmlBytes, savedFiling.getFilingReference() + ".xml", "str_reports_xml");
        }

        closureService.closeAfterStr(caseId, savedFiling.getId(), filedBy, ip);

        CaseAuditTrail trail = new CaseAuditTrail();
        trail.setCaseRecord(caseRecord);
        trail.setActorId(filedBy);
        trail.setEventType("STR_FILED");
        trail.setEventMetadata("{\"strFilingId\": \"" + savedFiling.getId() + "\", \"regulatoryBody\": \"" + savedFiling.getRegulatoryBody() + "\"}");
        trail.setIpAddress(ip);
        trailRepo.save(trail);

        String userEmail = resolveUserEmail(filedBy);
        eventPublisher.publishEvent(new StrFiledEvent(this, savedFiling.getFilingReference(), userEmail));

        return mapToResponseDto(savedFiling);
    }

    @Override
    @Transactional(readOnly = true)
    public StrFilingResponseDto getFilingDetail(UUID filingId) {
        ensureTenantContext();
        StrFiling filing = strRepo.findById(filingId)
                .orElseThrow(() -> new EntityNotFoundException("STR Filing not found"));
        return mapToResponseDto(filing);
    }

    /**
     * Generates the PDF "On-The-Fly" for the download endpoint.
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] getPdfReport(UUID filingId) {
        ensureTenantContext();

        StrFiling filing = strRepo.findById(filingId)
                .orElseThrow(() -> new EntityNotFoundException("STR Filing not found"));

        UUID caseId = filing.getCaseRecord().getId();
        List<Transaction> transactions = stRepo.findByStrFilingId(filingId);

        // Re-fetch the notes from the case
        List<CaseNote> notes = noteRepo.findByCaseRecordIdOrderBySysCreatedAtDesc(caseId);

        // Re-build evidence list
        List<String> evidence = transactions.stream()
                .map(t -> "Txn ID: " + t.getId() + " | Amount: " + t.getAmount())
                .toList();

        // Return the freshly generated byte array
        return docGen.generatePdf(filing, transactions, notes, evidence);
    }

    @Override
    @Transactional(readOnly = true)
    public void validateGate(UUID caseId) {
        ensureTenantContext();
        CaseRecord caseRecord = caseRepo.findById(caseId)
                .orElseThrow(() -> new EntityNotFoundException("Case not found"));

        if (!caseRecord.isHasInvestigationNote()) {
            throw new StrFilingGateException("Cannot file STR: Case must have at least one investigation note.");
        }
    }

    private StrFilingResponseDto mapToResponseDto(StrFiling filing) {
        return StrFilingResponseDto.builder()
                .id(filing.getId())
                .caseId(filing.getCaseRecord().getId())
                .filingReference(filing.getFilingReference())
                .regulatoryBody(filing.getRegulatoryBody())
                .typologyCategory(filing.getTypologyCategory())
                .subjectName(filing.getSubjectName())
                .subjectAccountNo(filing.getSubjectAccountNo())
                .suspicionNarrative(filing.getSuspicionNarrative())
                .filedBy(filing.getFiledBy())
                .sysCreatedAt(filing.getSysCreatedAt())
                .build();
    }

    private String resolveUserEmail(UUID userId) {
        return "compliance_" + userId.toString().substring(0, 8) + "@bank.com";
    }

    /**
     * Safety net to prevent "relation does not exist" errors when the Postman
     * header is missing, specifically recovering to the bank schema.
     */
    private void ensureTenantContext() {
        if (TenantContext.getTenantId() == null) {
            TenantContext.setTenantId("hdfc_bank_04_schema");
            log.info("TenantContext recovered to default schema in StrFilingServiceImpl");
        }
    }
}