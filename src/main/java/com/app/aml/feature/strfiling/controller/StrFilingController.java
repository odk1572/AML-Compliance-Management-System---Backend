package com.app.aml.feature.strfiling.controller;


import com.app.aml.domain.api.ApiResponse;
import com.app.aml.feature.strfiling.dto.strFiling.StrFilingRequestDto;
import com.app.aml.feature.strfiling.dto.strFiling.StrFilingResponseDto;
import com.app.aml.feature.strfiling.service.StrFilingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/str-filings")
@RequiredArgsConstructor
public class StrFilingController {

    private final StrFilingService strFilingService;

    /**
     * Files a Suspicious Transaction Report (STR/SAR) and automatically closes the case.
     */
    @PostMapping("/cases/{caseId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BANK_ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<StrFilingResponseDto>> fileSar(
            @PathVariable UUID caseId,
            @Valid @RequestBody StrFilingRequestDto dto,
            @RequestParam UUID filedBy,
            HttpServletRequest request) {

        // The IP is fetched automatically from the HTTP request to feed into the Audit Trail
        StrFilingResponseDto responseDto = strFilingService.fileSar(
                caseId,
                dto,
                filedBy,
                request.getRemoteAddr()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.of(
                        HttpStatus.CREATED,
                        "STR/SAR filed successfully, document generated, and case securely closed",
                        request.getRequestURI(),
                        responseDto
                )
        );
    }

    /**
     * Retrieves the details of a specific STR Filing by its ID.
     */
    @GetMapping("/{filingId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BANK_ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<StrFilingResponseDto>> getFilingDetail(
            @PathVariable UUID filingId,
            HttpServletRequest request) {

        StrFilingResponseDto responseDto = strFilingService.getFilingDetail(filingId);

        return ResponseEntity.ok(
                ApiResponse.of(
                        HttpStatus.OK,
                        "STR Filing details retrieved successfully",
                        request.getRequestURI(),
                        responseDto
                )
        );
    }


    @GetMapping(value = "/{filingId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BANK_ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<byte[]> downloadStrPdf(@PathVariable UUID filingId) {

        // Fetch the raw PDF bytes from your service
        byte[] pdfBytes = strFilingService.getPdfReport(filingId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);

        // Use "inline" to open in browser, or "attachment" to force download
        headers.setContentDispositionFormData("attachment", "STR-Report-" + filingId.toString().substring(0,8) + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}