package com.app.aml.feature.reporting.controller;

import com.app.aml.apiResponse.ApiResponse;
import com.app.aml.feature.reporting.dtos.TenantReportDtos.*;
import com.app.aml.feature.reporting.service.TenantReportService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenant-reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('BANK_ADMIN')")
public class TenantReportController {

    private final TenantReportService reportService;


    @GetMapping("/str-log")
    public ResponseEntity<ApiResponse<Page<StrLogDto>>> getStrLog(
            @PageableDefault(size = 20) Pageable pageable,
            HttpServletRequest request) {

        Page<StrLogDto> data = reportService.getStrLog(pageable);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "STR filing history log retrieved successfully",
                request.getRequestURI(),
                data
        ));
    }

    @GetMapping("/batch-summary")
    public ResponseEntity<ApiResponse<Page<BatchSummaryDto>>> getBatchSummary(
            @PageableDefault(size = 10) Pageable pageable,
            HttpServletRequest request) {

        Page<BatchSummaryDto> data = reportService.getBatchSummary(pageable);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Ingestion batch processing summary retrieved successfully",
                request.getRequestURI(),
                data
        ));
    }

    @GetMapping("/co-performance")
    public ResponseEntity<ApiResponse<List<CoPerformanceDto>>> getCoPerformance(
            HttpServletRequest request) {

        List<CoPerformanceDto> data = reportService.getCoPerformance();
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Compliance Officer performance analytics calculated successfully",
                request.getRequestURI(),
                data
        ));
    }
}