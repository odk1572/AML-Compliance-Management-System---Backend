package com.app.aml.feature.ingestion.controller;

import com.app.aml.domain.api.ApiResponse;
import com.app.aml.feature.ingestion.dto.transactionBatch.response.TransactionBatchResponseDto;
import com.app.aml.feature.ingestion.service.CustomerProfileBatchService;
import com.app.aml.security.userDetails.PlatformUserDetails;
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
            @AuthenticationPrincipal PlatformUserDetails userDetails) {

        // Trigger the service. The service returns instantly while Spring Batch runs in the background.
        TransactionBatchResponseDto responseDto = batchService.uploadAndTriggerBatch(file, userDetails.getId());

        // Assuming your standard ApiResponse wrapper
        ApiResponse<TransactionBatchResponseDto> response = ApiResponse.<TransactionBatchResponseDto>builder()
                .success(true)
                .message("Batch file uploaded successfully and is currently pending processing.")
                .data(responseDto)
                .build();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}