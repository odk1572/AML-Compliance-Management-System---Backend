package com.app.aml.shared.audit.controller;

import com.app.aml.apiResponse.ApiResponse;
import com.app.aml.shared.audit.service.AuditLogService;

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

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasAnyRole('BANK_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<?>>> getAllLogs(
            @PageableDefault(size = 20, sort = "sysCreatedAt") Pageable pageable,
            HttpServletRequest request) {

        Page<?> logs = auditLogService.getAuditLogs(pageable);

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Audit logs retrieved successfully.",
                request.getRequestURI(),
                logs
        ));
    }
}