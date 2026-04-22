package com.app.aml.feature.strfiling.dto.strFilingTransaction;

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
public class StrFilingTransactionResponseDto {
    private UUID id;
    private UUID strFilingId;   // Flattened from StrFiling
    private UUID transactionId; // Flattened from Transaction
    private Instant sysCreatedAt;
}
