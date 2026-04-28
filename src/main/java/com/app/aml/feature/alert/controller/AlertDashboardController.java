package com.app.aml.feature.alert.controller;


import com.app.aml.apiResponse.ApiResponse;
import com.app.aml.enums.AlertSeverity;
import com.app.aml.enums.AlertStatus;
import com.app.aml.feature.alert.dto.alert.AlertDetailResponseDto;
import com.app.aml.feature.alert.dto.alert.response.AlertResponseDto;
import com.app.aml.feature.alert.service.AlertDashboardService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('BANK_ADMIN')")
public class AlertDashboardController {

    private final AlertDashboardService alertDashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AlertResponseDto>>> getAlerts(
            @RequestParam(required = false) AlertSeverity severity,
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable pageable,
            HttpServletRequest request) {

        Page<AlertResponseDto> data = alertDashboardService.getAlerts(severity, status, from, to,Pageable.unpaged());
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Alerts retrieved successfully", request.getRequestURI(), data));
    }

    @GetMapping("/{alertId}")
    public ResponseEntity<ApiResponse<AlertDetailResponseDto>> getAlertDetail(
            @PathVariable UUID alertId,
            HttpServletRequest request) {

        AlertDetailResponseDto data = alertDashboardService.getAlertDetail(alertId);
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Alert details retrieved successfully", request.getRequestURI(), data));
    }

    @GetMapping("/severity-counts")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getSeverityCounts(HttpServletRequest request) {
        Map<String, Long> data = alertDashboardService.getSeverityCounts();
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Severity counts retrieved successfully", request.getRequestURI(), data));
    }

    @PatchMapping("/{alertId}/close")
    public ResponseEntity<ApiResponse<Void>> closeAlert(
            @PathVariable UUID alertId,
            @RequestParam AlertStatus resolution,
            @RequestParam(required = false) String comment,
            HttpServletRequest request) {

        alertDashboardService.closeAlert(alertId, resolution, comment);
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Alert closed successfully", request.getRequestURI(), null));
    }
}