package com.app.aml.feature.strfiling.dto.strFilingTransaction;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
public class StrFilingTransactionResponseDto {
    private UUID id;
    private UUID strFilingId;   // Flattened from StrFiling
    private UUID transactionId; // Flattened from Transaction
    private Instant sysCreatedAt;
}
