package com.app.aml.feature.ingestion.dto.transactionBatch.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransactionBatchRequestDto {

    @NotBlank(message = "Batch reference is required")
    @Size(max = 50, message = "Batch reference cannot exceed 50 characters")
    private String batchReference;

    @NotBlank(message = "File name is required")
    @Size(max = 255)
    private String fileName;

    @NotBlank(message = "File hash (SHA-256) is required for integrity")
    @Size(max = 64)
    private String fileHashSha256;

    @NotNull(message = "File size is required")
    private Long fileSizeBytes;

    @Size(max = 100)
    private String cloudinaryPublicId;

    private String cloudinarySecureUrl;

    // uploadedBy is omitted because your Service will extract it from the Security Context (JWT)
}