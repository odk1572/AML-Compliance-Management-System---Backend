package com.app.aml.feature.strfiling.dto.strFiling;

import com.app.aml.enums.TypologyCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class StrFilingRequestDto {
    @NotBlank
    private String regulatoryBody;

    @NotBlank
    private String suspicionNarrative;

}