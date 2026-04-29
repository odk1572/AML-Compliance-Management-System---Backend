package com.app.aml.feature.alert.repository;

import com.app.aml.enums.AlertSeverity;
import com.app.aml.enums.AlertStatus;
import com.app.aml.feature.alert.entity.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    List<Alert> findAllByAlertReferenceIn( List<String> alertReferences);

    @Query("""
    SELECT a FROM Alert a 
    WHERE (:severity IS NULL OR a.severity = :severity) 
      AND (:status IS NULL OR a.status = :status) 
      AND (a.sysCreatedAt BETWEEN :start AND :end)
""")
    Page<Alert> findWithFilters(
            @Param("severity") AlertSeverity severity,
            @Param("status") AlertStatus status,
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable);

    @Query("SELECT a.severity, COUNT(a) FROM Alert a WHERE a.status = :status GROUP BY a.severity")
    List<Object[]> countByStatusAndGroupBySeverity(@Param("status") AlertStatus status);
    List<Alert> findTop5ByCustomerIdOrderBySysCreatedAtDesc(UUID customerId);
}