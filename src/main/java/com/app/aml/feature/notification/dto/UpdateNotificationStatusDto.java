package com.app.aml.feature.notification.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateNotificationStatusDto {
    @NotNull
    private Boolean isRead;
}