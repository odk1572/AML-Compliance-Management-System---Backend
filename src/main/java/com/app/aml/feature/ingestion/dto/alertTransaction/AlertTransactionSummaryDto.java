package com.app.aml.feature.ingestion.dto.alertTransaction;


import com.app.aml.domain.enums.InvolvementRole;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

@Value // Immutable DTO
public class AlertTransactionSummaryDto {
    UUID transactionId;
    String transactionRef;
    BigDecimal amount;
    String currency;
    InvolvementRole role;
}