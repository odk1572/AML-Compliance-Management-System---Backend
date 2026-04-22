package com.app.aml.feature.notification.dto;


import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class BulkMarkAsReadRequestDto {
    @NotEmpty(message = "Notification IDs list cannot be empty")
    private List<UUID> notificationIds;
}