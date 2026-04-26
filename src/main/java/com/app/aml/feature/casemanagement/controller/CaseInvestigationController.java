package com.app.aml.feature.casemanagement.controller;

import com.app.aml.domain.api.ApiResponse;
import com.app.aml.feature.casemanagement.dto.caseAuditTrail.CaseAuditTrailResponseDto;
import com.app.aml.feature.casemanagement.dto.request.CaseNoteRequestDto;
import com.app.aml.feature.casemanagement.service.CaseInvestigationService;
import com.app.aml.feature.ingestion.dto.alert.response.AlertResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cases/investigation")
@RequiredArgsConstructor
public class CaseInvestigationController {

    private final CaseInvestigationService investigationService;

    @PatchMapping("/{caseId}/open")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BANK_ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<Void>> openCase(
            @PathVariable UUID caseId,
            @RequestParam UUID actorId,
            HttpServletRequest request) {

        investigationService.openCase(caseId, actorId, request.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Case status updated to IN_PROGRESS", request.getRequestURI(), null));
    }

    @PostMapping("/{caseId}/notes")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BANK_ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<Void>> addNote(
            @PathVariable UUID caseId,
            @Valid @RequestBody CaseNoteRequestDto dto,
            @RequestParam UUID authoredBy,
            HttpServletRequest request) {

        investigationService.addNote(caseId, dto, authoredBy, request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(HttpStatus.CREATED, "Investigation note added successfully", request.getRequestURI(), null));
    }

    @GetMapping("/{caseId}/audit-trail")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BANK_ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<List<CaseAuditTrailResponseDto>>> getAuditTrail(
            @PathVariable UUID caseId,
            HttpServletRequest request) {

        List<CaseAuditTrailResponseDto> data = investigationService.getCaseAuditTrail(caseId);
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Audit trail retrieved successfully", request.getRequestURI(), data));
    }

    @GetMapping(value = "/{caseId}/audit-trail/export", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BANK_ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<byte[]> exportAuditTrail(@PathVariable UUID caseId) {
        byte[] pdfContent = investigationService.exportAuditTrailAsPdf(caseId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=case_audit_trail_" + caseId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfContent);
    }

    @GetMapping("/{caseId}/alerts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BANK_ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<List<AlertResponseDto>>> getAlertsForCase(
            @PathVariable UUID caseId,
            HttpServletRequest request) {

        List<AlertResponseDto> data = investigationService.getAlertsForCase(caseId);
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Linked alerts retrieved successfully", request.getRequestURI(), data));
    }
}