package com.app.aml.feature.casemanagement.dto.caseNote;

import com.app.aml.domain.enums.NoteType;
import jakarta.validation.constraints.NotBlank;
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
public class CreateCaseNoteRequestDto {

    @NotNull(message = "Case ID is required")
    private UUID caseId;

    @NotNull(message = "Note type is required")
    private NoteType noteType;

    @NotBlank(message = "Note content cannot be empty")
    private String noteContent;
}