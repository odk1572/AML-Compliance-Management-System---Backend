package com.app.aml.feature.casemanagement.repository;

import com.app.aml.feature.casemanagement.entity.CaseAlertLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CaseAlertLinkRepository extends JpaRepository<CaseAlertLink, UUID> {
    @Query("SELECT cal FROM CaseAlertLink cal JOIN FETCH cal.alert WHERE cal.caseRecord.id = :caseId ORDER BY cal.sysCreatedAt ASC")
    List<CaseAlertLink> findByCaseRecordIdWithAlerts(@Param("caseId") UUID caseId);
}