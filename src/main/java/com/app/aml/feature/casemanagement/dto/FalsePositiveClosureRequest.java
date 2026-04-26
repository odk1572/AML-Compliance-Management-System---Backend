package com.app.aml.feature.casemanagement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FalsePositiveClosureRequest {
    @NotBlank(message = "Rationale cannot be blank")
    private String rationale;
}