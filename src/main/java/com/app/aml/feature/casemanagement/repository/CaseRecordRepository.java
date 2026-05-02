package com.app.aml.feature.casemanagement.repository;

import com.app.aml.enums.CaseStatus;
import com.app.aml.feature.casemanagement.entity.CaseRecord;
import com.app.aml.feature.ingestion.entity.CustomerProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CaseRecordRepository extends JpaRepository<CaseRecord, UUID> {

    Optional<CaseRecord> findByCaseReference(String caseReference);

    @Modifying
    @Query("UPDATE CaseRecord c SET c.assignedTo = null WHERE c.assignedTo = :userId AND c.status NOT IN ('CLOSED', 'ARCHIVED')")
    void unassignOpenCasesForUser(@Param("userId") UUID userId);

    Page<CaseRecord> findByCustomerIdOrderBySysCreatedAtDesc(UUID customerId, Pageable pageable);


    List<CaseRecord> findByStatusIn(Collection<CaseStatus> statuses);

    boolean existsByCustomerAndStatusInAndCaseTransactions_Transaction_IdIn(
            CustomerProfile customer,
            List<CaseStatus> statuses,
            List<UUID> transactionIds
    );

}
