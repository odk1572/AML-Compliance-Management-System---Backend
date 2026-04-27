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
import java.util.ArrayList;
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

        var primaryAlertLink = caseRecord.getCaseAlertLinks().stream()
                .filter(link -> link.isPrimaryAlert())
                .findFirst()
                .orElse(caseRecord.getCaseAlertLinks().get(0)); // Fallback to first alert if no primary marked

        var alert = primaryAlertLink.getAlert();

        List<Transaction> transactionsToLink = new ArrayList<>();
        transactionsToLink = caseRecord.getCaseTransactions().stream()
                    .map(CaseTransaction::getTransaction)
                    .toList();

        if (transactionsToLink.isEmpty()) {
            throw new IllegalArgumentException("Cannot file STR: No transactions linked to Case.");
        }

        StrFiling filing = new StrFiling();
        filing.setCaseRecord(caseRecord);
        filing.setFilingReference("STR-" + Instant.now().toEpochMilli());
        filing.setRegulatoryBody(dto.getRegulatoryBody());
        filing.setTypologyCategory(dto.getTypologyCategory());

        filing.setSubjectName(alert.getTypologyTriggered().split("-")[0]); // Example: If typology is "John Doe - Velocity"
        filing.setSubjectAccountNo(transactionsToLink.get(0).getOriginatorAccountNo());

        filing.setSuspicionNarrative(dto.getSuspicionNarrative());
        filing.setFiledBy(filedBy);

        StrFiling savedFiling = strRepo.save(filing);

        for (Transaction transaction : transactionsToLink) {
            StrFilingTransaction filingTransaction = new StrFilingTransaction();
            filingTransaction.setStrFiling(savedFiling);
            filingTransaction.setTransaction(transaction);
            stRepo.save(filingTransaction);
        }

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

        return mapToResponseDto(savedFiling, transactionsToLink);
    }

    @Override
    @Transactional(readOnly = true)
    public StrFilingResponseDto getFilingDetail(UUID filingId) {
        ensureTenantContext();
        StrFiling filing = strRepo.findById(filingId)
                .orElseThrow(() -> new EntityNotFoundException("STR Filing not found"));

        List<Transaction> transactions = stRepo.findByStrFilingId(filingId).stream()
                .map(StrFilingTransaction::getTransaction)
                .toList();

        return mapToResponseDto(filing, transactions);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getPdfReport(UUID filingId) {
        ensureTenantContext();

        StrFiling filing = strRepo.findById(filingId)
                .orElseThrow(() -> new EntityNotFoundException("STR Filing not found"));

        List<Transaction> transactions = stRepo.findByStrFilingId(filingId).stream()
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
    public void validateGate(UUID caseId) {
        ensureTenantContext();
        CaseRecord caseRecord = caseRepo.findById(caseId)
                .orElseThrow(() -> new EntityNotFoundException("Case not found"));

        if (!caseRecord.isHasInvestigationNote()) {
            throw new StrFilingGateException("Cannot file STR: Case must have at least one investigation note.");
        }
    }

    private StrFilingResponseDto mapToResponseDto(StrFiling filing, List<Transaction> transactions) {
        List<StrFilingResponseDto.LinkedTransactionDto> transactionDtos = transactions.stream()
                .map(txn -> StrFilingResponseDto.LinkedTransactionDto.builder()
                        .id(txn.getId())
                        .transactionReference(txn.getTransactionRef())
                        .amount(txn.getAmount())
                        .currency(txn.getCurrencyCode())
                        .timestamp(txn.getTransactionTimestamp())
                        .originatorAccount(txn.getOriginatorAccountNo())
                        .beneficiaryAccount(txn.getBeneficiaryAccountNo())
                        .build())
                .toList();

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
                .transactions(transactionDtos)
                .build();
    }

    private String resolveUserEmail(UUID userId) {
        return "compliance_" + userId.toString().substring(0, 8) + "@bank.com";
    }

    private void ensureTenantContext() {
        if (TenantContext.getTenantId() == null) {
            log.error("Multi-tenancy violation: No Tenant ID found in current context.");
            throw new IllegalStateException("Tenant context is missing. Request cannot be processed.");
        }
    }
}