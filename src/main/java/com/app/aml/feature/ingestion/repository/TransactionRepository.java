package com.app.aml.feature.ingestion.repository;

import com.app.aml.feature.ingestion.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    boolean existsByTransactionRef(String ref);

    @Query("SELECT t.transactionRef FROM Transaction t")
    List<String> findAllTransactionRefs();
    Page<Transaction> findByOriginatorAccountNoOrBeneficiaryAccountNo(String originatorAccountNo, String beneficiaryAccountNo, Pageable pageable);

    @Query(value =
            "SELECT originator_account_no FROM transactions WHERE UPPER(originator_name) = UPPER(:name) " +
                    "UNION " +
                    "SELECT beneficiary_account_no FROM transactions WHERE UPPER(beneficiary_name) = UPPER(:name)",
            nativeQuery = true)
    List<String> findLinkedAccountsByName(@Param("name") String name);


    Optional<Transaction> findByTransactionRef(String transactionRef);
}