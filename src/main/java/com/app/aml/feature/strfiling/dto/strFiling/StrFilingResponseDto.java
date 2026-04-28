package com.app.aml.feature.strfiling.dto.strFiling;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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
    private UUID caseId;
    private String filingReference;
    private String regulatoryBody;


    private String ruleType;
    private String typologyTriggered;

    private String suspicionNarrative;
    private UUID filedBy;
    private Instant sysCreatedAt;

    private StrCustomerDto customer;
    private List<LinkedTransactionDto> transactions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StrCustomerDto {
        private UUID id;
        private String accountNumber;
        private String customerName;
        private String riskRating;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinkedTransactionDto {
        private UUID id;
        private String transactionReference;
        private BigDecimal amount;
        private String currency;
        private Instant transactionTimestamp;
        private String transactionType;
        private String originatorAccount;
        private String beneficiaryAccount;
    }
}