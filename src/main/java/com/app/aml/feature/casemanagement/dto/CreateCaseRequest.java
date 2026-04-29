package com.app.aml.feature.casemanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreateCaseRequest {

    @NotEmpty(message = "At least one alert reference must be provided")
    private List<String> alertReferences; 

    @NotBlank(message = "Assignee user code is required")
    private String assigneeUserCode;

    @NotBlank(message = "Priority is required")
    private String priority;
}