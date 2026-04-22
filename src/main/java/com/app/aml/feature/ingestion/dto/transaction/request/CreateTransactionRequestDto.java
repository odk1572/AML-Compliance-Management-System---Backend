package com.app.aml.feature.ingestion.dto.transaction.request;

import com.app.aml.domain.enums.Channel;
import com.app.aml.domain.enums.TransactionType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CreateTransactionRequestDto {

    @NotNull(message = "Batch ID is required")
    private UUID batchId;

    private UUID customerId; // Optional: The system might not map it to a known profile immediately

    @NotBlank(message = "Transaction reference is required")
    @Size(max = 100, message = "Transaction reference cannot exceed 100 characters")
    private String transactionRef;

    @Size(max = 50)
    private String originatorAccountNo;

    @Size(max = 255)
    private String originatorName;

    @Size(max = 50)
    private String originatorBankCode;

    @Size(max = 3, message = "Originator country must be a 3-letter ISO code")
    private String originatorCountry;

    @Size(max = 50)
    private String beneficiaryAccountNo;

    @Size(max = 255)
    private String beneficiaryName;

    @Size(max = 50)
    private String beneficiaryBankCode;

    @Size(max = 3, message = "Beneficiary country must be a 3-letter ISO code")
    private String beneficiaryCountry;

    @NotNull(message = "Amount is required")
    @Digits(integer = 18, fraction = 2)
    private BigDecimal amount;

    @NotBlank(message = "Currency code is required")
    @Size(max = 3, message = "Currency code must be a 3-letter ISO code")
    private String currencyCode;

    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;

    @NotNull(message = "Channel is required")
    private Channel channel;

    @NotNull(message = "Transaction timestamp is required")
    private Instant transactionTimestamp;

    private String referenceNote;

    // Note: 'status' is omitted because the entity defaults to CLEAN upon creation.
}