package com.app.aml.feature.investigation.service;
import com.app.aml.annotation.AuditAction;
import com.app.aml.feature.ingestion.dto.transaction.response.TransactionResponseDto;
import com.app.aml.feature.ingestion.entity.Transaction;
import com.app.aml.feature.ingestion.mapper.TransactionMapper;
import com.app.aml.feature.ingestion.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionInvestigationServiceImpl implements TransactionInvestigationService {

    private final TransactionRepository transactionRepo;
    private final TransactionMapper transactionMapper;

    @Override
    @AuditAction(category = "DATA_ACCESS", action = "VIEW_TRANSACTION_LIST", entityType = "TRANSACTION")
    public Page<TransactionResponseDto> getAllTransactions(Pageable pageable) {
        return transactionRepo.findAll(pageable)
                .map(transactionMapper::toResponseDto);
    }

    @Override
    @AuditAction(category = "DATA_ACCESS", action = "VIEW_TRANSACTION_DETAILS", entityType = "TRANSACTION")
    public TransactionResponseDto getTransactionDetails(String transactionRef) {
        Transaction transaction = transactionRepo.findByTransactionRef(transactionRef)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + transactionRef));

        return transactionMapper.toResponseDto(transaction);
    }
}