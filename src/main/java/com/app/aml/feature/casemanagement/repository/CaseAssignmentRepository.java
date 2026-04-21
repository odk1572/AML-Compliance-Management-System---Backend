package com.app.aml.feature.casemanagement.repository;

import com.app.aml.feature.casemanagement.entity.CaseAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface CaseAssignmentRepository extends JpaRepository<CaseAssignment, UUID> {
}