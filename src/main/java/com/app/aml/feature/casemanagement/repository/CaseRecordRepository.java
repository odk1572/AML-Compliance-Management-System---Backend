package com.app.aml.feature.casemanagement.repository;

import com.app.aml.domain.enums.CaseStatus;
import com.app.aml.feature.casemanagement.entity.CaseRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface CaseRecordRepository extends JpaRepository<CaseRecord, UUID> {

    @Modifying
    @Query("UPDATE CaseRecord c SET c.assignedTo = null WHERE c.assignedTo = :userId AND c.status NOT IN ('CLOSED', 'ARCHIVED')")
    void unassignOpenCasesForUser(@Param("userId") UUID userId);
    // Assuming CaseRecord has a mapped collection of alerts, e.g., @OneToMany List<Alert> alerts;

    // Spring Data will automatically translate this to a SELECT WHERE customer_profile_id = ?
    List<CaseRecord> findByCustomerProfileIdOrderBySysCreatedAtDesc(UUID customerProfileId, Pageable pageable);

    @Query("SELECT DISTINCT c FROM CaseRecord c " +
            "JOIN CaseAlertLink cal ON c.id = cal.caseRecord.id " +
            "JOIN cal.alert a " +
            "WHERE a.customerProfileId = :profileId " +
            "ORDER BY c.openedAt DESC")
    List<CaseRecord> findRecentCasesByCustomerProfileId(@Param("profileId") UUID customerProfileId, Pageable pageable);

    List<CaseRecord> findByStatusIn(Collection<CaseStatus> statuses);
}
