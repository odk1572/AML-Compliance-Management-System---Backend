package com.app.aml.feature.ingestion.dto.transactionBatch.request;

import com.app.aml.enums.BatchStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTransactionBatchProgressDto {

    @NotNull(message = "Batch status is required")
    private BatchStatus batchStatus;

    private Integer totalRecords;

    private String failureDetails;

    @Size(max = 100)
    private String springBatchJobId;

    private Instant processedAt;
}