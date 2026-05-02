package com.app.aml.feature.casemanagement.dto.caseRecord.request;

import com.app.aml.enums.CasePriority;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCaseRequestDto {

    private UUID assignedTo;

    @NotNull(message = "Initial priority is required")
    private CasePriority priority;

}