package com.app.aml.feature.casemanagement.dto.caseNote;

import com.app.aml.enums.NoteType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CaseNoteResponseDto {
    private UUID id;
    private UUID caseId;
    private UUID authoredBy;
    private NoteType noteType;
    private String noteContent;
    private Instant sysCreatedAt;
}