package com.app.aml.feature.ingestion.controller;

import com.app.aml.domain.api.ApiResponse;
import com.app.aml.feature.ingestion.dto.transactionBatch.response.TransactionBatchResponseDto;
import com.app.aml.feature.ingestion.service.CustomerProfileBatchService;
import com.app.aml.security.userDetails.PlatformUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/customer-profiles/batch")
@RequiredArgsConstructor
public class CustomerProfileBatchController {

    private final CustomerProfileBatchService batchService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TransactionBatchResponseDto>> uploadCustomerBatch(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal PlatformUserDetails userDetails,
            HttpServletRequest request) {

        TransactionBatchResponseDto responseDto = batchService.uploadAndTriggerBatch(
                file,
                userDetails.getPlatformUser().getId()
        );

        ApiResponse<TransactionBatchResponseDto> response = ApiResponse.of(
                HttpStatus.ACCEPTED,
                "Batch file uploaded successfully and is currently pending processing.",
                request.getRequestURI(),
                responseDto
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}