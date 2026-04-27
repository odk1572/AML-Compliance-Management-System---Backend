package com.app.aml.feature.strfiling.dto.strFiling;

import com.app.aml.enums.TypologyCategory;
import com.app.aml.feature.casemanagement.dto.caseRecord.response.CaseResponseDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
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
    private List<LinkedTransactionDto> transactions;
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinkedTransactionDto {
        private UUID id;
        private String transactionReference;
        private java.math.BigDecimal amount;
        private String currency;
        private Instant timestamp;
        private String originatorAccount;
        private String beneficiaryAccount;
    }
}

