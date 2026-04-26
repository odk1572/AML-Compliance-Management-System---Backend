package com.app.aml.feature.casemanagement.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ReassignCaseRequest {
    @NotNull
    private UUID newAssigneeId;
    @NotNull
    private UUID reassignedById;
    @NotBlank
    private String reason;
}