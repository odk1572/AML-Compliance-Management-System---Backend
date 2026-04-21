package com.app.aml.feature.casemanagement.repository;

import com.app.aml.feature.casemanagement.entity.CaseEscalation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface CaseEscalationRepository extends JpaRepository<CaseEscalation, UUID> {
}