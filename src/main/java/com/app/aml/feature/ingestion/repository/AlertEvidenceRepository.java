package com.app.aml.feature.ingestion.repository;

import com.app.aml.domain.enums.AlertSeverity;
import com.app.aml.domain.enums.AlertStatus;
import com.app.aml.feature.ingestion.entity.AlertEvidence;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AlertEvidenceRepository extends JpaRepository<AlertEvidence, UUID> {
    List<AlertEvidence> findByAlertId(UUID alertId);;
}
