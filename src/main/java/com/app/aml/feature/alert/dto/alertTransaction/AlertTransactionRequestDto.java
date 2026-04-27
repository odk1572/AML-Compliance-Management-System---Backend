package com.app.aml.feature.alert.dto.alertTransaction;



import com.app.aml.enums.InvolvementRole;
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
public class AlertTransactionRequestDto {

    @NotNull(message = "Alert ID is required")
    private UUID alertId;

    @NotNull(message = "Transaction ID is required")
    private UUID transactionId;

    @NotNull(message = "Involvement role must be specified (TRIGGER or CONTRIBUTOR)")
    private InvolvementRole involvementRole;
}