package com.app.aml.feature.casemanagement.dto.request;

import com.app.aml.domain.enums.NoteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CaseNoteRequestDto {
    @NotNull
    private NoteType noteType;

    @NotBlank
    private String noteContent;
}