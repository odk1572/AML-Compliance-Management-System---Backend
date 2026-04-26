package com.app.aml.feature.casemanagement.dto;


import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class StrClosureRequest {
    @NotNull(message = "Filing ID cannot be null")
    private UUID filingId;
}