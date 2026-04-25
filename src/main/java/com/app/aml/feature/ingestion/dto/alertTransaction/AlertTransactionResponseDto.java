package com.app.aml.feature.ingestion.dto.alertTransaction;


import com.app.aml.domain.enums.InvolvementRole;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class AlertTransactionResponseDto {
    private UUID id; // The link ID

    // Transaction Details
    private UUID transactionId;
    private String transactionRef;
    private BigDecimal amount;
    private String currencyCode;
    private Instant transactionTimestamp;
    private String originatorAccountNo;
    private String beneficiaryAccountNo;

    // Link Metadata
    private InvolvementRole involvementRole;
    private Instant sysCreatedAt;
}