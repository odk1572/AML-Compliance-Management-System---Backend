package com.app.aml.feature.strfiling.repository;

import com.app.aml.feature.ingestion.entity.Transaction;
import com.app.aml.feature.strfiling.entity.StrFilingTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StrFilingTransactionRepository extends JpaRepository<StrFilingTransaction, UUID> {
    List<Transaction> findByStrFilingId(UUID filingId);
}