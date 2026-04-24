package com.app.aml.feature.ingestion.controller;

import com.app.aml.domain.api.ApiResponse;
import com.app.aml.domain.enums.BatchFileType;
import com.app.aml.feature.ingestion.dto.transactionBatch.response.TransactionBatchResponseDto;
import com.app.aml.feature.ingestion.service.UniversalBatchIngestionService;
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
@RequestMapping("/api/v1/ingestion")
@RequiredArgsConstructor
public class UniversalIngestionController {

    private final UniversalBatchIngestionService ingestionService;



//     POST /api/v1/ingestion/CUSTOMER_PROFILE/upload
//     POST /api/v1/ingestion/TRANSACTION/upload
    @PostMapping(value = "/{fileType}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TransactionBatchResponseDto>> uploadBatchFile(
            @PathVariable BatchFileType fileType,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal PlatformUserDetails userDetails,
            HttpServletRequest request) {

        TransactionBatchResponseDto responseDto = ingestionService.uploadAndRouteBatch(
                file,
                userDetails.getPlatformUser().getId(),
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
}