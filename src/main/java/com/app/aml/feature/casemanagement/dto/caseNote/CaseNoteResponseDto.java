package com.app.aml.feature.casemanagement.dto.caseNote;

import com.app.aml.domain.enums.NoteType;
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
public class CaseNoteResponseDto {
    private UUID id;
    private UUID caseId; // Flattened from CaseRecord
    private UUID authoredBy;
    private NoteType noteType;
    private String noteContent;
    private Instant sysCreatedAt;
}