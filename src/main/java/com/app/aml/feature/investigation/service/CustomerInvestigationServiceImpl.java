package com.app.aml.feature.investigation.service;
import com.app.aml.feature.casemanagement.repository.CaseRecordRepository;
import com.app.aml.feature.ingestion.dto.customerProfile.response.CustomerProfileResponseDto;
import com.app.aml.feature.ingestion.dto.customerProfile.response.TransactionSummaryDto;
import com.app.aml.feature.ingestion.entity.CustomerProfile;
import com.app.aml.feature.alert.mapper.AlertMapper; // Assuming you have this
import com.app.aml.feature.casemanagement.mapper.CaseRecordMapper; // Assuming you have this
import com.app.aml.feature.alert.repository.AlertRepository;
import com.app.aml.feature.ingestion.repository.CustomerProfileRepository;
import com.app.aml.feature.ingestion.repository.TransactionRepository;
import com.app.aml.annotation.AuditAction;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerInvestigationServiceImpl implements CustomerInvestigationService {

    private final CustomerProfileRepository customerRepo;
    private final TransactionRepository txnRepo;
    private final CaseRecordRepository caseRepo;
    private final AlertRepository alertRepo;

    private final AlertMapper alertMapper;
    private final CaseRecordMapper caseRecordMapper;

    @Override
    @AuditAction(category = "DATA_ACCESS", action = "VIEW_CUSTOMER_360", entityType = "CUSTOMER")
    public CustomerProfileResponseDto get360View(String accountNo) {
        CustomerProfile profile = customerRepo.findByAccountNumber(accountNo)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found for account: " + accountNo));


        var recentAlerts = alertRepo.findTop5ByCustomerIdOrderBySysCreatedAtDesc(profile.getId())
                .stream().map(alertMapper::toResponseDto).toList();

        var recentCases = caseRepo.findByCustomerIdOrderBySysCreatedAtDesc(profile.getId(),Pageable.unpaged())
                .stream().map(caseRecordMapper::toResponseDto).toList();

        return CustomerProfileResponseDto.builder()
                .id(profile.getId())
                .accountNumber(profile.getAccountNumber())
                .customerName(profile.getCustomerName())
                .customerType(profile.getCustomerType())
                .nationality(profile.getNationality())
                .countryOfResidence(profile.getCountryOfResidence())
                .riskRating(profile.getRiskRating())
                .riskScore(profile.getRiskScore())
                .kycStatus(profile.getKycStatus())
                .accountOpenedOn(profile.getAccountOpenedOn())
                .lastActivityDate(profile.getLastActivityDate())
                .isDormant(profile.isDormant())
                .isPep(profile.isPep())
                .recentAlerts(recentAlerts)
                .recentCases(recentCases)
                .build();
    }

    @Override
    @AuditAction(category = "DATA_ACCESS", action = "VIEW_CUSTOMER_TRANSACTION_HISTORY", entityType = "CUSTOMER")
    public Page<TransactionSummaryDto> getTransactionHistory(String accountNo, Pageable pageable) {
        return txnRepo.findByOriginatorAccountNoOrBeneficiaryAccountNo(accountNo, accountNo, pageable)
                .map(txn -> TransactionSummaryDto.builder()
                        .transactionId(txn.getId())
                        .transactionRef(txn.getTransactionRef())
                        .amount(txn.getAmount())
                        .currency(txn.getCurrencyCode())
                        .txnDate(txn.getTransactionTimestamp())
                        .txnType(txn.getTransactionType().name())
                        .build());
    }

    @Override
    @AuditAction(category = "DATA_ACCESS", action = "VIEW_LINKED_ACCOUNTS", entityType = "CUSTOMER")
    public List<String> getLinkedAccounts(String accountNo) {
        CustomerProfile profile = customerRepo.findByAccountNumber(accountNo)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found for account: " + accountNo));

        return txnRepo.findLinkedAccountsByName(profile.getCustomerName());
    }

    @Override
    @AuditAction(category = "DATA_ACCESS", action = "LIST_ALL_CUSTOMERS", entityType = "CUSTOMER")
    public Page<CustomerProfileResponseDto> getAllCustomers(Pageable pageable) {

        return customerRepo.findAll(pageable)
                .map(profile -> CustomerProfileResponseDto.builder()
                        .id(profile.getId())
                        .accountNumber(profile.getAccountNumber())
                        .customerName(profile.getCustomerName())
                        .customerType(profile.getCustomerType())
                        .riskRating(profile.getRiskRating())
                        .riskScore(profile.getRiskScore())
                        .kycStatus(profile.getKycStatus())
                        .nationality(profile.getNationality())
                        .accountOpenedOn(profile.getAccountOpenedOn())
                        .lastActivityDate(profile.getLastActivityDate())
                        .isPep(profile.isPep())
                        .build());
    }
}