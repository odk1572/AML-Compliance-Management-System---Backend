package com.app.aml.feature.casemanagement.repository;

import com.app.aml.feature.casemanagement.entity.CaseNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CaseNoteRepository extends JpaRepository<CaseNote, UUID> {
    List<CaseNote> findByCaseRecordIdOrderBySysCreatedAtDesc(UUID caseId);
}