package com.app.aml.feature.ingestion.dto.transactionBatch.response;

import com.app.aml.domain.enums.BatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionBatchResponseDto {
    private UUID id;
    private String batchReference;
    private UUID uploadedBy;
    private String fileName;
    private String fileHashSha256;
    private Long fileSizeBytes;
    private String cloudinaryPublicId;
    private String cloudinarySecureUrl;
    private Integer totalRecords;
    private BatchStatus batchStatus;
    private String failureDetails; // Will contain the JSON string of errors
    private String springBatchJobId;
    private Instant processedAt;
    private Instant sysCreatedAt;
    private Instant sysUpdatedAt;
}