package com.app.aml.feature.casemanagement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReassignCaseRequest {

    @NotBlank(message = "New assignee user code is required")
    private String newAssigneeUserCode; // e.g., "USR-ALEX-02"

    @NotBlank(message = "A reason for reassignment must be provided")
    private String reason;
}