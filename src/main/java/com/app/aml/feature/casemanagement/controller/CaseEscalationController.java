package com.app.aml.feature.casemanagement.controller;


import com.app.aml.apiResponse.ApiResponse;
import com.app.aml.feature.casemanagement.dto.caseEscalation.request.EscalationRequestDto;
import com.app.aml.feature.casemanagement.service.CaseEscalationService;
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
public class CaseEscalationController {

    private final CaseEscalationService caseEscalationService;

    @PostMapping("/{caseId}/escalate")
    @PreAuthorize("hasAnyRole('BANK_ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<Void>> escalateCase(
            @PathVariable UUID caseId,
            @Valid @RequestBody EscalationRequestDto requestDto,
            @RequestParam UUID escalatedById,
            HttpServletRequest request) {

        caseEscalationService.escalate(
                caseId,
                requestDto,
                escalatedById,
                request.getRemoteAddr()
        );

        return ResponseEntity.ok(
                ApiResponse.of(
                        HttpStatus.OK,
                        "Case escalated successfully",
                        request.getRequestURI(),
                        null
                )
        );
    }
}