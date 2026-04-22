package com.app.aml.feature.ingestion.dto.transaction.response;

import com.app.aml.domain.enums.Channel;
import com.app.aml.domain.enums.TransactionStatus;
import com.app.aml.domain.enums.TransactionType;
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
public class TransactionResponseDto {
    private UUID id;
    private UUID batchId;     // Flattened from TransactionBatch
    private UUID customerId;  // Flattened from CustomerProfile
    private String transactionRef;
    private String originatorAccountNo;
    private String originatorName;
    private String originatorBankCode;
    private String originatorCountry;
    private String beneficiaryAccountNo;
    private String beneficiaryName;
    private String beneficiaryBankCode;
    private String beneficiaryCountry;
    private BigDecimal amount;
    private String currencyCode;
    private TransactionType transactionType;
    private Channel channel;
    private Instant transactionTimestamp;
    private String referenceNote;
    private TransactionStatus status;
    private Instant sysCreatedAt;
}