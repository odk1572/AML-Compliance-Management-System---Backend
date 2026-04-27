package com.app.aml.feature.casemanagement.controller;


import com.app.aml.apiResponse.ApiResponse;
import com.app.aml.feature.casemanagement.dto.CreateCaseRequest;
import com.app.aml.feature.casemanagement.dto.ReassignCaseRequest;
import com.app.aml.feature.casemanagement.dto.caseRecord.response.CaseResponseDto;

import com.app.aml.feature.casemanagement.service.CaseAssignmentService;
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
public class CaseAssignmentController {

    private final CaseAssignmentService caseAssignmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BANK_ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<CaseResponseDto>> createCase(
            @Valid @RequestBody CreateCaseRequest request,
            HttpServletRequest httpRequest) {


        CaseResponseDto data = caseAssignmentService.createCase(
                request.getAlertIds(),
                request.getAssigneeId(),
                request.getAssignedById(),
                request.getPriority()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.of(
                        HttpStatus.CREATED,
                        "Case created and assigned successfully",
                        httpRequest.getRequestURI(),
                        data
                )
        );
    }

    @GetMapping("/{caseId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BANK_ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<CaseResponseDto>> getCaseDetails(
            @PathVariable UUID caseId,
            HttpServletRequest httpRequest) {

        CaseResponseDto data = caseAssignmentService.getCaseDetails(caseId);

        return ResponseEntity.ok(
                ApiResponse.of(
                        HttpStatus.OK,
                        "Case details retrieved successfully",
                        httpRequest.getRequestURI(),
                        data
                )
        );
    }

    @PatchMapping("/{caseId}/reassign")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BANK_ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<Void>> reassignCase(
            @PathVariable UUID caseId,
            @Valid @RequestBody ReassignCaseRequest request,
            HttpServletRequest httpRequest) {

        caseAssignmentService.reassignCase(
                caseId,
                request.getNewAssigneeId(),
                request.getReassignedById(),
                request.getReason()
        );

        return ResponseEntity.ok(
                ApiResponse.of(
                        HttpStatus.OK,
                        "Case reassigned successfully",
                        httpRequest.getRequestURI(),
                        null
                )
        );
    }
}