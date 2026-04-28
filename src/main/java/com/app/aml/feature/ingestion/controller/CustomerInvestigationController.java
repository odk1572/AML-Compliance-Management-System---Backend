package com.app.aml.feature.ingestion.controller;

import com.app.aml.apiResponse.ApiResponse;
import com.app.aml.feature.ingestion.dto.customerProfile.response.CustomerProfileResponseDto;
import com.app.aml.feature.ingestion.dto.customerProfile.response.TransactionSummaryDto;
import com.app.aml.feature.ingestion.service.CustomerInvestigationService;
import com.app.aml.annotation.AuditAction;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerInvestigationController {

    private final CustomerInvestigationService customerService;

    @GetMapping("/{accountNo}/360")
    @AuditAction(category = "DATA_ACCESS", action = "VIEW_CUSTOMER_360", entityType = "CUSTOMER")
    public ResponseEntity<ApiResponse<CustomerProfileResponseDto>> getCustomer360(
            @PathVariable String accountNo,
            HttpServletRequest request) {

        CustomerProfileResponseDto data = customerService.get360View(accountNo);

        ApiResponse<CustomerProfileResponseDto> response = ApiResponse.of(
                HttpStatus.OK,
                "Customer forensic 360 view retrieved successfully.",
                request.getRequestURI(),
                data
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountNo}/transactions")
    @AuditAction(category = "DATA_ACCESS", action = "VIEW_CUSTOMER_TRANSACTIONS", entityType = "CUSTOMER")
    public ResponseEntity<ApiResponse<Page<TransactionSummaryDto>>> getTransactionHistory(
            @PathVariable String accountNo,
            Pageable pageable,
            HttpServletRequest request) {

        Page<TransactionSummaryDto> data = customerService.getTransactionHistory(accountNo, pageable);

        ApiResponse<Page<TransactionSummaryDto>> response = ApiResponse.of(
                HttpStatus.OK,
                "Transaction history retrieved successfully.",
                request.getRequestURI(),
                data
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountNo}/links")
    @AuditAction(category = "DATA_ACCESS", action = "VIEW_CUSTOMER_LINKS", entityType = "CUSTOMER")
    public ResponseEntity<ApiResponse<List<String>>> getLinkedAccounts(
            @PathVariable String accountNo,
            HttpServletRequest request) {

        List<String> data = customerService.getLinkedAccounts(accountNo);

        ApiResponse<List<String>> response = ApiResponse.of(
                HttpStatus.OK,
                "Linked accounts retrieved based on profile matching.",
                request.getRequestURI(),
                data
        );

        return ResponseEntity.ok(response);
    }
}