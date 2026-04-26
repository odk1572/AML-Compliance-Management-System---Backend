package com.app.aml.feature.casemanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class EscalationRequestDto {
    @NotNull
    private UUID escalatedTo;

    @NotBlank
    private String escalationReason;
}