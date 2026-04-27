package com.app.aml.feature.ingestion.dto.transaction.request;

import com.app.aml.enums.TransactionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTransactionStatusRequestDto {

    @NotNull(message = "Transaction status is required")
    private TransactionStatus status;
}