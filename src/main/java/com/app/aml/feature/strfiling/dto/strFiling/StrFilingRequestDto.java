package com.app.aml.feature.strfiling.dto.strFiling;
import com.app.aml.domain.enums.TypologyCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class StrFilingRequestDto {
    @NotBlank
    private String regulatoryBody;
    @NotNull
    private TypologyCategory typologyCategory;
    @NotBlank
    private String subjectName;
    @NotBlank
    private String subjectAccountNo;
    @NotBlank
    private String suspicionNarrative;
    @NotEmpty
    private List<UUID> transactionIds;
}