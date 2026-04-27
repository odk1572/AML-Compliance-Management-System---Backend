package com.app.aml.feature.alert.dto.alertTransaction;


import com.app.aml.enums.InvolvementRole;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlertTransactionResponseDto {
    private UUID id;
    private UUID transactionId;
    private String transactionRef;
    private BigDecimal amount;
    private String currencyCode;
    private Instant transactionTimestamp;
    private String originatorAccountNo;
    private String beneficiaryAccountNo;
    private InvolvementRole involvementRole;
    private Instant sysCreatedAt;
}