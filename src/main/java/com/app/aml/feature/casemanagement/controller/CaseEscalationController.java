package com.app.aml.feature.casemanagement.controller;

import com.app.aml.apiResponse.ApiResponse;
import com.app.aml.feature.casemanagement.dto.caseEscalation.request.EscalationRequestDto;
import com.app.aml.feature.casemanagement.service.CaseEscalationService;

import com.app.aml.utils.SecurityUtils;
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

    // Notice the path variable is now {caseReference} instead of {caseId}
    @PostMapping("/{caseReference}/escalate")
    @PreAuthorize("hasAnyRole('BANK_ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<Void>> escalateCase(
            @PathVariable String caseReference,
            @Valid @RequestBody EscalationRequestDto requestDto,
            HttpServletRequest request) {

        UUID escalatedById = SecurityUtils.getCurrentUserId();

        caseEscalationService.escalate(
                caseReference,
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