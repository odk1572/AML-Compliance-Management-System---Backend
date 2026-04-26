package com.app.aml.feature.ingestion.repository;
import com.app.aml.feature.ingestion.entity.AlertTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertTransactionRepository extends JpaRepository<AlertTransaction, UUID> {

    List<AlertTransaction> findAllByAlertId(UUID alertId);
    List<AlertTransaction> findByAlertId(UUID alertId);



    List<AlertTransaction> findAllByTransactionId(UUID transactionId);
}