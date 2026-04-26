package com.app.aml.feature.reporting.dtos;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PlatformReportDtos {

    @Data @Builder @AllArgsConstructor
    public static class SarSummaryDto {
        private String tenantName;
        private long sarCount;
        private BigDecimal totalReportedAmount;
    }

    @Data @Builder @AllArgsConstructor
    public static class RuleEffectivenessDto {
        private String ruleId;
        private String ruleName;
        private long confirmedHits;
        private long falsePositives;
        private double effectivenessRatio;
    }

    @Data @Builder @AllArgsConstructor
    public static class AlertTrendDto {
        private LocalDate date;
        private long alertCount;
    }

    @Data @Builder @AllArgsConstructor
    public static class GeoHeatmapDto {
        private String countryCode;
        private long transactionCount;
        private BigDecimal totalVolume;
        private String type; // ORIGINATOR or BENEFICIARY
    }

    @Data @Builder @AllArgsConstructor
    public static class CoWorkloadDto {
        private String coName;
        private long activeCases;
        private long closedCases;
    }
}