package com.app.aml.feature.ingestion.service;
import com.app.aml.feature.casemanagement.repository.CaseRecordRepository;
import com.app.aml.feature.ingestion.dto.customerProfile.response.CustomerProfileResponseDto;
import com.app.aml.feature.ingestion.dto.customerProfile.response.TransactionSummaryDto;
import com.app.aml.feature.ingestion.entity.CustomerProfile;
import com.app.aml.feature.alert.mapper.AlertMapper; // Assuming you have this
import com.app.aml.feature.casemanagement.mapper.CaseRecordMapper; // Assuming you have this
import com.app.aml.feature.alert.repository.AlertRepository;
import com.app.aml.feature.ingestion.repository.CustomerProfileRepository;
import com.app.aml.feature.ingestion.repository.TransactionRepository;
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
    public CustomerProfileResponseDto get360View(String accountNo) {
        CustomerProfile profile = customerRepo.findByAccountNumber(accountNo)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found for account: " + accountNo));


        var recentAlerts = alertRepo.findTop5ByCustomerProfileIdOrderBySysCreatedAtDesc(profile.getId())
                .stream().map(alertMapper::toResponseDto).toList();

        var recentCases = caseRepo.findByCustomerProfileIdOrderBySysCreatedAtDesc(profile.getId(),Pageable.unpaged())
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
    public List<String> getLinkedAccounts(String accountNo) {
        CustomerProfile profile = customerRepo.findByAccountNumber(accountNo)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found for account: " + accountNo));

        return txnRepo.findLinkedAccountsByName(profile.getCustomerName());
    }
}