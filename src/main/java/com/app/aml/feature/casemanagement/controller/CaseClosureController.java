package com.app.aml.feature.casemanagement.controller;


import com.app.aml.domain.api.ApiResponse;
import com.app.aml.feature.casemanagement.dto.FalsePositiveClosureRequest;
import com.app.aml.feature.casemanagement.dto.StrClosureRequest;
import com.app.aml.feature.casemanagement.service.CaseClosureService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cases")
@RequiredArgsConstructor
public class CaseClosureController {

    private final CaseClosureService caseClosureService;

    @PostMapping("/{caseId}/close/false-positive")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BANK_ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<Void>> closeAsFalsePositive(
            @PathVariable UUID caseId,
            @Valid @RequestBody FalsePositiveClosureRequest requestDto,
            @RequestParam UUID closedBy,
            HttpServletRequest request) {

        caseClosureService.closeAsFalsePositive(
                caseId,
                requestDto.getRationale(),
                closedBy,
                request.getRemoteAddr()
        );

        return ResponseEntity.ok(
                ApiResponse.of(
                        HttpStatus.OK,
                        "Case closed successfully as False Positive",
                        request.getRequestURI(),
                        null
                )
        );
    }

    @PostMapping("/{caseId}/close/str")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BANK_ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<Void>> closeAfterStr(
            @PathVariable UUID caseId,
            @Valid @RequestBody StrClosureRequest requestDto,
            @RequestParam UUID closedBy,
            HttpServletRequest request) {

        caseClosureService.closeAfterStr(
                caseId,
                requestDto.getFilingId(),
                closedBy,
                request.getRemoteAddr()
        );

        return ResponseEntity.ok(
                ApiResponse.of(
                        HttpStatus.OK,
                        "Case closed successfully after STR filing",
                        request.getRequestURI(),
                        null
                )
        );
    }
}