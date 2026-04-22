package com.app.aml.feature.strfiling.dto.strFiling;

import com.app.aml.domain.enums.TypologyCategory;
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
public class StrFilingResponseDto {
    private UUID id;
    private UUID caseId; // Flattened from CaseRecord
    private String filingReference;
    private String regulatoryBody;
    private TypologyCategory typologyCategory;
    private String subjectName;
    private String subjectAccountNo;
    private String suspicionNarrative;
    private UUID filedBy;
    private Instant sysCreatedAt;
}