package com.app.aml.feature.casemanagement.dto.caseRecord.request;

import com.app.aml.domain.enums.ClosureDisposition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloseCaseRequestDto {

    @NotNull(message = "Closure disposition is required")
    private ClosureDisposition closureDisposition;

    // Mandating rationale if the analyst claims it's not a real crime
    @NotBlank(message = "Rationale is required for closing a case")
    private String falsePositiveRationale;
}