package com.app.aml.feature.ingestion.repository;

import com.app.aml.feature.ingestion.entity.TransactionBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface TransactionBatchRepository extends JpaRepository<TransactionBatch, UUID> {
    boolean existsByFileHashSha256(String fileHash);

    Page<TransactionBatch> findByUploadedByOrderBySysCreatedAtDesc(UUID uploadedBy, Pageable pageable);
}