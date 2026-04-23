package com.app.aml.feature.casemanagement.repository;

import com.app.aml.feature.casemanagement.entity.CaseRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface CaseRecordRepository extends JpaRepository<CaseRecord, UUID> {

    @Modifying
    @Query("UPDATE CaseRecord c SET c.assignedTo = null WHERE c.assignedTo = :userId AND c.status NOT IN ('CLOSED', 'ARCHIVED')")
    void unassignOpenCasesForUser(@Param("userId") UUID userId);
}