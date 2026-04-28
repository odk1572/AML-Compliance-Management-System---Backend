package com.app.aml.feature.reporting.controller;
import com.app.aml.apiResponse.ApiResponse;
import com.app.aml.feature.reporting.dtos.PlatformReportDtos.*;
import com.app.aml.feature.reporting.service.PlatformReportService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/platform-reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PlatformReportController {

    private final PlatformReportService reportService;
    @GetMapping("/sar-summary")

    public ResponseEntity<ApiResponse<List<SarSummaryDto>>> getSarSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest request) {

        LocalDate endDate = (to != null) ? to : LocalDate.now();
        LocalDate startDate = (from != null) ? from : endDate.minusDays(30);

        List<SarSummaryDto> data = reportService.getSarSummary(startDate, endDate);

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Cross-tenant SAR summary retrieved successfully",
                request.getRequestURI(),
                data
        ));
    }



}