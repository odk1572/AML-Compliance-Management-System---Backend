package com.app.aml.feature.investigation.service;


import com.app.aml.feature.ingestion.dto.customerProfile.response.CustomerProfileResponseDto;
import com.app.aml.feature.ingestion.dto.customerProfile.response.TransactionSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomerInvestigationService {
    CustomerProfileResponseDto get360View(String accountNo);
    Page<TransactionSummaryDto> getTransactionHistory(String accountNo, Pageable pageable);
    List<String> getLinkedAccounts(String accountNo);
    Page<CustomerProfileResponseDto> getAllCustomers(Pageable pageable);
}