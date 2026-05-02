package com.app.aml.feature.strfiling.repository;

import com.app.aml.feature.strfiling.entity.StrFiling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StrFilingRepository extends JpaRepository<StrFiling, UUID> {
        Optional<StrFiling> findByCaseRecordId(UUID caseId);
}