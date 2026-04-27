package com.app.aml.feature.ingestion.controller;

import com.app.aml.apiResponse.ApiResponse;
import com.app.aml.enums.BatchFileType;
import com.app.aml.feature.ingestion.dto.transactionBatch.response.TransactionBatchResponseDto;
import com.app.aml.feature.ingestion.service.UniversalBatchIngestionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ingestion")
@RequiredArgsConstructor
public class UniversalIngestionController {

    private final UniversalBatchIngestionService ingestionService;

    @PostMapping(value = "/{fileType}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TransactionBatchResponseDto>> uploadBatchFile(
            @PathVariable BatchFileType fileType,
            @RequestParam("file") MultipartFile file,
            Authentication authentication,
            HttpServletRequest request) {

        UUID uploaderId = extractBankAdminId(authentication);

        TransactionBatchResponseDto responseDto = ingestionService.uploadAndRouteBatch(
                file,
                uploaderId,
                fileType
        );

        ApiResponse<TransactionBatchResponseDto> response = ApiResponse.of(
                HttpStatus.ACCEPTED,
                fileType.name() + " batch file uploaded successfully and is currently pending processing.",
                request.getRequestURI(),
                responseDto
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/batches/{batchId}")
    public ResponseEntity<ApiResponse<TransactionBatchResponseDto>> getBatchStatus(
            @PathVariable UUID batchId,
            HttpServletRequest request) {

        TransactionBatchResponseDto responseDto = ingestionService.getBatchStatus(batchId);

        ApiResponse<TransactionBatchResponseDto> response = ApiResponse.of(
                HttpStatus.OK,
                "Batch status retrieved successfully.",
                request.getRequestURI(),
                responseDto
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/batches")
    public ResponseEntity<ApiResponse<Page<TransactionBatchResponseDto>>> getAllBatches(
            @RequestParam(required = false) BatchFileType fileType,
            @PageableDefault(size = 10, sort = "sysCreatedAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {

        Page<TransactionBatchResponseDto> responsePage = ingestionService.getAllBatches(fileType, pageable);

        ApiResponse<Page<TransactionBatchResponseDto>> response = ApiResponse.of(
                HttpStatus.OK,
                "Batch history retrieved successfully.",
                request.getRequestURI(),
                responsePage
        );

        return ResponseEntity.ok(response);
    }


    private UUID extractBankAdminId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}