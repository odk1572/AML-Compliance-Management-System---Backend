package com.app.aml.feature.ingestion.repository;

import com.app.aml.feature.ingestion.entity.AlertEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface AlertEvidenceRepository extends JpaRepository<AlertEvidence, UUID> {
}