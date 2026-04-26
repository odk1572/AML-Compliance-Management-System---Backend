package com.app.aml.feature.casemanagement.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateCaseRequest {
    @NotEmpty
    private List<UUID> alertIds;
    @NotNull
    private UUID assigneeId;
    @NotNull
    private UUID assignedById;
    @NotBlank
    private String priority;
}