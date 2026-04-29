package com.app.aml.feature.investigation.service;


import com.app.aml.feature.ingestion.dto.transaction.response.TransactionResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionInvestigationService {
    Page<TransactionResponseDto> getAllTransactions(Pageable pageable);
    TransactionResponseDto getTransactionDetails(String transactionRef);
}