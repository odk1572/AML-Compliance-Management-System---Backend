package com.app.aml.feature.ingestion.controller;

import com.app.aml.domain.api.ApiResponse;
import com.app.aml.domain.enums.BatchFileType;
import com.app.aml.feature.ingestion.dto.transactionBatch.response.TransactionBatchResponseDto;
import com.app.aml.feature.ingestion.service.UniversalBatchIngestionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // <-- Use standard Authentication
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
            Authentication authentication, // <-- Inject this instead
            HttpServletRequest request) {

        // Extract the ID dynamically based on your Security context.
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

    /**
     * Helper method to safely extract the UUID from the Authentication object.
     */
    private UUID extractBankAdminId(Authentication authentication) {
        // Option A: If your JWT filter sets the username/name as the UUID string
        // return UUID.fromString(authentication.getName());

        // Option B: If you have a specific UserDetails class for Bank Admins (Uncomment and update)
        /*
        Object principal = authentication.getPrincipal();
        if (principal instanceof BankAdminUserDetails adminDetails) {
            return adminDetails.getId(); // Or whatever method gets the UUID
        }
        throw new IllegalStateException("Authenticated user is not a Bank Admin");
        */

        // For now, I am assuming your JWT sets the principal "name" as the ID string.
        // Update this based on how your team stores the user ID in the security context!
        return UUID.fromString(authentication.getName());
    }
}