package com.app.aml.feature.ingestion.dto.customerProfile.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionSummaryDto {
    private UUID transactionId;
    private String transactionRef;
    private BigDecimal amount;
    private String currency;
    private Instant txnDate;
    private String txnType;
}