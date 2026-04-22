package com.app.aml.feature.strfiling.dto.strFiling;

import com.app.aml.domain.enums.TypologyCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStrFilingRequestDto {

    @NotNull(message = "Case ID is required to link this filing")
    private UUID caseId;

    @NotBlank(message = "Regulatory body is required (e.g., FinCEN)")
    @Size(max = 50)
    private String regulatoryBody;

    @NotNull(message = "Typology category is required")
    private TypologyCategory typologyCategory;

    @NotBlank(message = "Subject name is required")
    @Size(max = 255)
    private String subjectName;

    @NotBlank(message = "Subject account number is required")
    @Size(max = 50)
    private String subjectAccountNo;

    @NotBlank(message = "A detailed suspicion narrative is required for regulators")
    private String suspicionNarrative;
}