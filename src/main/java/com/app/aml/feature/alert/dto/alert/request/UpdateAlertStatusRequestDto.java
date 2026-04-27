package com.app.aml.feature.alert.dto.alert.request;

import com.app.aml.enums.AlertStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAlertStatusRequestDto {
    @NotNull(message = "Alert status is required")
    private AlertStatus status;
}