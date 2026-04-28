package com.app.aml.feature.alert.dto.alertTransaction;


import com.app.aml.enums.InvolvementRole;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Value // Immutable DTO
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlertTransactionSummaryDto {
    UUID transactionId;
    String transactionRef;
    BigDecimal amount;
    String currency;
    Instant transactionTimestamp; // NEW
    String transactionType;       // NEW (e.g., TRANSFER, CASH)
    InvolvementRole role;
}