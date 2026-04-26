package com.app.aml.feature.ingestion.repository;

import com.app.aml.domain.enums.AlertSeverity;
import com.app.aml.domain.enums.AlertStatus;
import com.app.aml.feature.ingestion.entity.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {
    @Query("SELECT a FROM Alert a WHERE " +
            "(:severity IS NULL OR a.severity = :severity) AND " +
            "(:status IS NULL OR a.status = :status) AND " +
            "(a.sysCreatedAt BETWEEN :start AND :end)")
    Page<Alert> findWithFilters(
            @Param("severity") AlertSeverity severity,
            @Param("status") AlertStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);

    @Query("SELECT a.severity, COUNT(a) FROM Alert a WHERE a.status = :status GROUP BY a.severity")
    List<Object[]> countByStatusAndGroupBySeverity(@Param("status") AlertStatus status);
    List<Alert> findTop5ByCustomerProfileIdOrderBySysCreatedAtDesc(UUID customerProfileId);
}