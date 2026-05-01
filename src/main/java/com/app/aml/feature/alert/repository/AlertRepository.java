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

    @Query("SELECT COUNT(a) > 0 FROM Alert a " +
            "JOIN a.alertTransactions at " +
            "WHERE a.customer.id = :customerId " +
            "AND at.transaction.id IN :transactionIds")
    boolean existsByCustomerIdAndTransactionIds(
            @Param("customerId") UUID customerId,
            @Param("transactionIds") List<UUID> transactionIds
    );

    @Query("SELECT COUNT(a) > 0 FROM Alert a " +
            "JOIN a.alertTransactions at " +
            "WHERE a.customer.id = :customerId " +
            "AND a.ruleType = :ruleType " +
            "AND at.transaction.id IN :transactionIds")
    boolean existsByCustomerRuleAndTransactions(
            @Param("customerId") UUID customerId,
            @Param("ruleType") String ruleType,
            @Param("transactionIds") List<UUID> transactionIds
    );

    @Query("""
    SELECT a FROM Alert a 
    WHERE (:severity IS NULL OR a.severity = :severity) 
      AND (:status IS NULL OR a.status = :status) 
      AND (a.sysCreatedAt BETWEEN :start AND :end)
      AND (:customer IS NULL OR LOWER(a.customer.customerName) LIKE :customer OR LOWER(a.customer.accountNumber) LIKE :customer)
""")
    Page<Alert> findWithFilters(
            @Param("severity") AlertSeverity severity,
            @Param("status") AlertStatus status,
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable,
            @Param("customer") String customer);

    @Query("SELECT a.severity, COUNT(a) FROM Alert a WHERE a.status = :status GROUP BY a.severity")
    List<Object[]> countByStatusAndGroupBySeverity(@Param("status") AlertStatus status);
    List<Alert> findTop5ByCustomerIdOrderBySysCreatedAtDesc(UUID customerId);
}