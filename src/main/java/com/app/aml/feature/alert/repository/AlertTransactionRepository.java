package com.app.aml.feature.alert.repository;
import com.app.aml.feature.alert.entity.AlertTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertTransactionRepository extends JpaRepository<AlertTransaction, UUID> {

    List<AlertTransaction> findByAlertId(UUID alertId);
}