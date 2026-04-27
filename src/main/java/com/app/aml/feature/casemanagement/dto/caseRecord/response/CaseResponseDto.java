package com.app.aml.feature.casemanagement.dto.caseRecord.response;

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
public class CaseResponseDto {
    private UUID id;
    private String caseReference;
    private String status;
    private String priority;
    private Integer aggregatedRiskScore;
    private UUID assignedTo;
    private Instant openedAt;

    private List<LinkedTransactionDto> transactions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinkedTransactionDto {
        private UUID id;
        private String transactionReference;
        private BigDecimal amount;
        private String currency;
        private Instant timestamp;
        private String originatorAccount;
        private String beneficiaryAccount;
    }
}