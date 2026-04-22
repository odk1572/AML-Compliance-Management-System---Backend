package com.app.aml.feature.strfiling.dto.strFilingTransaction;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStrFilingTransactionRequestDto {

    @NotNull(message = "STR Filing ID is required")
    private UUID strFilingId;

    @NotNull(message = "Transaction ID is required")
    private UUID transactionId;
}