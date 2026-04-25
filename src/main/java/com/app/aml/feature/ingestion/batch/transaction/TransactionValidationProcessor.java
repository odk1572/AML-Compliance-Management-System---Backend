package com.app.aml.feature.ingestion.batch.transaction;

import com.app.aml.domain.enums.Channel;
import com.app.aml.domain.enums.TransactionType;
import com.app.aml.feature.ingestion.batch.ValidationException;
import com.app.aml.feature.ingestion.entity.CustomerProfile;
import com.app.aml.feature.ingestion.entity.Transaction;
import com.app.aml.feature.ingestion.entity.TransactionBatch;
import com.app.aml.feature.ingestion.repository.CustomerProfileRepository;
import com.app.aml.feature.ingestion.repository.TransactionBatchRepository;
import com.app.aml.feature.ingestion.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

import static com.app.aml.feature.ingestion.batch.util.BatchValidationUtils.*;

@Component
@RequiredArgsConstructor
public class TransactionValidationProcessor implements ItemProcessor<TransactionCsvDto, Transaction> {

    private final TransactionRepository transactionRepository;
    private final CustomerProfileRepository customerRepository;
    private final TransactionBatchRepository batchRepository;

    private TransactionBatch currentBatch;

    // Intercept the JobParameters to get the current Batch entity once per step
    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        String batchIdStr = stepExecution.getJobParameters().getString("batchId");
        if (batchIdStr != null) {
            this.currentBatch = batchRepository.findById(UUID.fromString(batchIdStr))
                    .orElseThrow(() -> new IllegalStateException("Batch not found"));
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

        // 3. Amount Validation (Must be > 0)
        BigDecimal amount = parseBigDecimal(dto.getAmount(), line, "amount");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(line, "amount", "Transaction amount must be strictly positive");
        }
        txn.setAmount(amount);

        // 4. ISO-8601 Timestamp Validation
        txn.setTransactionTimestamp(parseInstant(dto.getTransactionTimestamp(), line, "transactionTimestamp"));

        // 5. Enums
        txn.setTransactionType(parseEnum(TransactionType.class, dto.getTransactionType(), line, "transactionType"));
        txn.setChannel(parseEnum(Channel.class, dto.getChannel(), line, "channel"));

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

        txn.setReferenceNote(dto.getReferenceNote()); // TEXT field, no severe length limit

// ... (Previous steps 1 to 6 remain the same) ...

        // 7. Strict Tenant Boundary & Customer Resolution
        boolean isOriginatorInternal = false;
        boolean isBeneficiaryInternal = false;
        CustomerProfile primaryCustomer = null;

        // Check Originator
        if (txn.getOriginatorAccountNo() != null) {
            CustomerProfile origProfile = customerRepository.findByAccountNumber(txn.getOriginatorAccountNo()).orElse(null);
            if (origProfile != null) {
                isOriginatorInternal = true;
                primaryCustomer = origProfile; // Default to Originator as primary owner
            }
        }

        // Check Beneficiary
        if (txn.getBeneficiaryAccountNo() != null) {
            CustomerProfile benProfile = customerRepository.findByAccountNumber(txn.getBeneficiaryAccountNo()).orElse(null);
            if (benProfile != null) {
                isBeneficiaryInternal = true;
                if (primaryCustomer == null) {
                    primaryCustomer = benProfile; // Make Beneficiary primary if Originator is external
                }
            }
        }


        // Rule A: The "Orphan" Check (Neither belongs to tenant)
        if (!isOriginatorInternal && !isBeneficiaryInternal) {
            throw new ValidationException(line, "accountNo", "Invalid Transaction: Neither originator nor beneficiary belongs to this tenant.");
        }

        // Rule B: The "Circular / Internal Transfer" Check
        // If the transaction claims both sides are within the same bank, BOTH must exist in your DB.
        boolean isSameBank = txn.getOriginatorBankCode() != null &&
                txn.getOriginatorBankCode().equalsIgnoreCase(txn.getBeneficiaryBankCode());

        if (isSameBank && (!isOriginatorInternal || !isBeneficiaryInternal)) {
            throw new ValidationException(line, "accountNo", "Circular Transfer Validation Failed: Both accounts must exist in the tenant database for internal transfers.");
        }

        // Link the transaction to the verified tenant customer
        txn.setCustomer(primaryCustomer);

        return txn;
    }
}