package com.app.aml.feature.ingestion.batch.transaction;

import com.app.aml.domain.enums.Channel;
import com.app.aml.domain.enums.TransactionStatus; // Added import
import com.app.aml.domain.enums.TransactionType;
import com.app.aml.feature.ingestion.batch.ValidationException;
import com.app.aml.feature.ingestion.entity.CustomerProfile;
import com.app.aml.feature.ingestion.entity.Transaction;
import com.app.aml.feature.ingestion.entity.TransactionBatch;
import com.app.aml.feature.ingestion.repository.CustomerProfileRepository;
import com.app.aml.feature.ingestion.repository.TransactionBatchRepository;
import com.app.aml.feature.ingestion.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

import static com.app.aml.feature.ingestion.batch.util.BatchValidationUtils.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionValidationProcessor implements ItemProcessor<TransactionCsvDto, Transaction> {

    private final TransactionRepository transactionRepository;
    private final CustomerProfileRepository customerRepository;
    private final TransactionBatchRepository batchRepository;

    private TransactionBatch currentBatch;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        String batchIdStr = stepExecution.getJobParameters().getString("batchId");
        if (batchIdStr != null) {
            this.currentBatch = batchRepository.findById(UUID.fromString(batchIdStr))
                    .orElseThrow(() -> new IllegalStateException("Batch not found for ID: " + batchIdStr));
        }
    }

    @Override
    public Transaction process(TransactionCsvDto dto) {
        int line = dto.getLineNumber();
        Transaction txn = new Transaction();

        // 1. Batch Assignment
        txn.setBatch(currentBatch);

        // 2. Transaction Reference & Idempotency Check
        String ref = require(dto.getTransactionRef(), line, "transactionRef", 100);
        if (transactionRepository.existsByTransactionRef(ref)) {
            throw new ValidationException(line, "transactionRef", "Duplicate transaction reference found in system");
        }
        txn.setTransactionRef(ref);

        // 3. Amount Validation
        BigDecimal amount = parseBigDecimal(dto.getAmount(), line, "amount");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(line, "amount", "Transaction amount must be strictly positive");
        }
        txn.setAmount(amount);

        // 4. Timestamp Validation
        txn.setTransactionTimestamp(parseInstant(dto.getTransactionTimestamp(), line, "transactionTimestamp"));

        // 5. Enums (Type & Channel)// Use a default value like TransactionType.TRANSFER or Channel.ONLINE if parsing fails
        txn.setTransactionType(parseEnum(TransactionType.class, dto.getTransactionType(), line, "transactionType", TransactionType.TRANSFER));
        //
        txn.setChannel(parseEnum(Channel.class, dto.getChannel(), line, "channel", Channel.ONLINE));

        // 6. Basic String Constraints
        txn.setCurrencyCode(require(dto.getCurrencyCode(), line, "currencyCode", 3));
        txn.setOriginatorAccountNo(safe(dto.getOriginatorAccountNo(), 50));
        txn.setOriginatorName(safe(dto.getOriginatorName(), 255));
        txn.setOriginatorBankCode(safe(dto.getOriginatorBankCode(), 50));
        txn.setOriginatorCountry(safe(dto.getOriginatorCountry(), 3));
        txn.setBeneficiaryAccountNo(safe(dto.getBeneficiaryAccountNo(), 50));
        txn.setBeneficiaryName(safe(dto.getBeneficiaryName(), 255));
        txn.setBeneficiaryBankCode(safe(dto.getBeneficiaryBankCode(), 50));
        txn.setBeneficiaryCountry(safe(dto.getBeneficiaryCountry(), 3));
        txn.setReferenceNote(dto.getReferenceNote());

        // 7. Strict Tenant Boundary & Customer Resolution
        boolean isOriginatorInternal = false;
        boolean isBeneficiaryInternal = false;
        CustomerProfile primaryCustomer = null;

        if (txn.getOriginatorAccountNo() != null) {
            CustomerProfile origProfile = customerRepository.findByAccountNumber(txn.getOriginatorAccountNo()).orElse(null);
            if (origProfile != null) {
                isOriginatorInternal = true;
                primaryCustomer = origProfile;
            }
        }

        if (txn.getBeneficiaryAccountNo() != null) {
            CustomerProfile benProfile = customerRepository.findByAccountNumber(txn.getBeneficiaryAccountNo()).orElse(null);
            if (benProfile != null) {
                isBeneficiaryInternal = true;
                if (primaryCustomer == null) {
                    primaryCustomer = benProfile;
                }
            }
        }

        // Rule A: The "Orphan" Check
        // CRITICAL: If your customer table is empty, this is why you get 1000 skips!
        if (!isOriginatorInternal && !isBeneficiaryInternal) {
            throw new ValidationException(line, "accountNo", "Invalid Transaction: Neither originator nor beneficiary belongs to this tenant.");
        }

        // Rule B: Internal Transfer Check
        boolean isSameBank = txn.getOriginatorBankCode() != null &&
                txn.getOriginatorBankCode().equalsIgnoreCase(txn.getBeneficiaryBankCode());

        if (isSameBank && (!isOriginatorInternal || !isBeneficiaryInternal)) {
            throw new ValidationException(line, "accountNo", "Circular Transfer Validation Failed: Both accounts must exist for internal transfers.");
        }

        txn.setCustomer(primaryCustomer);

        // 8. Status Mapping (New Step)
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            try {
                txn.setStatus(TransactionStatus.valueOf(dto.getStatus().toUpperCase().trim()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status {} at line {}. Defaulting to CLEAN.", dto.getStatus(), line);
                txn.setStatus(TransactionStatus.CLEAN);
            }
        } else {
            txn.setStatus(TransactionStatus.CLEAN);
        }

        return txn;
    }
}