package com.app.aml.feature.investigation.controller;


import com.app.aml.apiResponse.ApiResponse;
import com.app.aml.feature.ingestion.dto.transaction.response.TransactionResponseDto;
import com.app.aml.feature.investigation.service.TransactionInvestigationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/investigation/transactions")
@RequiredArgsConstructor
public class TransactionInvestigationController {

    private final TransactionInvestigationService investigationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('BANK_ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<Page<TransactionResponseDto>>> getAllTransactions(
            @PageableDefault(size = 20, sort = "transactionTimestamp") Pageable pageable,
            HttpServletRequest request) {

        Page<TransactionResponseDto> data = investigationService.getAllTransactions(pageable);

        return ResponseEntity.ok(
                ApiResponse.of(
                        HttpStatus.OK,
                        "Transactions retrieved successfully",
                        request.getRequestURI(),
                        data
                )
        );
    }


    @GetMapping("/{transactionRef}")
    @PreAuthorize("hasAnyRole('BANK_ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> getTransactionDetails(
            @PathVariable String transactionRef,
            HttpServletRequest request) {

        TransactionResponseDto data = investigationService.getTransactionDetails(transactionRef);

        return ResponseEntity.ok(
                ApiResponse.of(
                        HttpStatus.OK,
                        "Transaction details retrieved successfully",
                        request.getRequestURI(),
                        data
                )
        );
    }
}