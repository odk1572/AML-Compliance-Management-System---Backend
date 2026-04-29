package com.app.aml.feature.casemanagement.controller;


import com.app.aml.apiResponse.ApiResponse;
import com.app.aml.feature.casemanagement.dto.CreateCaseRequest;
import com.app.aml.feature.casemanagement.dto.ReassignCaseRequest;
import com.app.aml.feature.casemanagement.dto.caseRecord.response.CaseResponseDto;

import com.app.aml.feature.casemanagement.service.CaseAssignmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

    @GetMapping
    @PreAuthorize("hasAnyRole('BANK_ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<Page<CaseResponseDto>>> getAllCases(
            @PageableDefault(size = 20, sort = "lastActivityAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {

        Page<CaseResponseDto> data = caseAssignmentService.getAllCases(pageable);

        return ResponseEntity.ok(
                ApiResponse.of(
                        HttpStatus.OK,
                        "All cases retrieved successfully",
                        request.getRequestURI(),
                        data
                )
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<ApiResponse<CaseResponseDto>> createCase(
            @Valid @RequestBody CreateCaseRequest request,
            HttpServletRequest httpRequest) {


        CaseResponseDto data = caseAssignmentService.createCase(
                request
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

    @GetMapping("/{caseRef}")
    @PreAuthorize("hasAnyRole('BANK_ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<CaseResponseDto>> getCaseDetails(
            @PathVariable String caseRef,
            HttpServletRequest httpRequest) {

        CaseResponseDto data = caseAssignmentService.getCaseDetails(caseRef);

        return ResponseEntity.ok(
                ApiResponse.of(
                        HttpStatus.OK,
                        "Case details retrieved successfully",
                        httpRequest.getRequestURI(),
                        data
                )
        );
    }

    @PatchMapping("/{caseRef}/reassign")
    @PreAuthorize("hasAnyRole('BANK_ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<Void>> reassignCase(
            @PathVariable String caseRef,
            @Valid @RequestBody ReassignCaseRequest request,
            HttpServletRequest httpRequest) {

        caseAssignmentService.reassignCase(
                caseRef,request
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