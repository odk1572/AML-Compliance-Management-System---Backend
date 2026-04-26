package com.app.aml.feature.reporting.dtos;


import com.app.aml.domain.enums.CaseStatus;
import com.app.aml.domain.enums.TypologyCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class TenantReportDtos {

    @Data @Builder @AllArgsConstructor
    public static class FlaggedTransactionDto {
        private UUID transactionId;
        private String accountNo;
        private BigDecimal amount;
        private String currency;
        private String riskLevel;
        private Instant timestamp;
    }

    @Data @Builder @AllArgsConstructor
    public static class StrLogDto {
        private String filingReference;
        private TypologyCategory typology;
        private String subjectName;
        private UUID filedBy;
        private Instant filedAt;
    }

    @Data @Builder @AllArgsConstructor
    public static class BatchSummaryDto {
        private UUID batchId;
        private String fileName;
        private String status;
        private long totalRecords;
        private long failedRecords;
        private Instant uploadedAt;
    }

    @Data @Builder @AllArgsConstructor
    public static class CoPerformanceDto {
        private UUID coId;
        private String coName; // Placeholder or resolved via User Service
        private Double avgDaysToClose;
        private Long totalClosed;
    }
}