package com.app.aml.feature.ingestion.entity;

import com.app.aml.domain.enums.BatchStatus;
import com.app.aml.shared.audit.SoftDeletableEntity;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transaction_batches")
@Getter
@Setter
@NoArgsConstructor
public class TransactionBatch extends SoftDeletableEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @NotBlank
    @Size(max = 50)
    @Column(name = "batch_reference", unique = true, nullable = false, length = 50)
    private String batchReference;

    @NotNull
    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @NotBlank
    @Size(max = 255)
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @NotBlank
    @Size(max = 64)
    @Column(name = "file_hash_sha256", nullable = false, length = 64)
    private String fileHashSha256;

    @NotNull
    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Size(max = 100)
    @Column(name = "cloudinary_public_id", length = 100)
    private String cloudinaryPublicId;

    @Column(name = "cloudinary_secure_url", columnDefinition = "TEXT")
    private String cloudinarySecureUrl;

    @Column(name = "total_records")
    private Integer totalRecords = 0;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "batch_status", nullable = false, length = 50)
    private BatchStatus batchStatus = BatchStatus.PENDING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "failure_details", columnDefinition = "jsonb")
    private String failureDetails;

    @Size(max = 100)
    @Column(name = "spring_batch_job_id", length = 100)
    private String springBatchJobId;

    @Column(name = "processed_at")
    private Instant processedAt;
}