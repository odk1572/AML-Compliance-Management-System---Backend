package com.app.aml.feature.ingestion.batch.customer;

import com.app.aml.domain.enums.CustomerType;
import com.app.aml.feature.ingestion.batch.ValidationException;
import com.app.aml.feature.ingestion.entity.CustomerProfile;
import com.app.aml.feature.ingestion.repository.CustomerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.batch.item.ItemProcessor;

import static com.app.aml.feature.ingestion.batch.util.BatchValidationUtils.*;

@Component
@RequiredArgsConstructor
public class CustomerProfileValidationProcessor implements ItemProcessor<CustomerProfileCsvDto, CustomerProfile> {

    private final CustomerProfileRepository customerProfileRepository;

    @Override
    public CustomerProfile process(CustomerProfileCsvDto dto) {

        int line = dto.getLineNumber();
        CustomerProfile profile = new CustomerProfile();

        // Account Number
//        String accNum = BatchValidationUtils.require(dto.getAccountNumber(), line, "accountNumber");
        String accNum = require(dto.getAccountNumber(), line, "accountNumber");

        if (customerProfileRepository.existsByAccountNumber(accNum)) {
            throw new ValidationException(line, "accountNumber", "Account Number already exists in system");
        }
        profile.setAccountNumber(accNum);

        // Name
        profile.setCustomerName(require(dto.getCustomerName(), line, "customerName"));

        // Customer Type
        String type = require(dto.getCustomerType(), line, "customerType");
        try {
            profile.setCustomerType(CustomerType.valueOf(type.toUpperCase()));
        } catch (Exception e) {
            throw new ValidationException(line, "customerType", "Invalid value: " + type);
        }

        // Financials
        profile.setMonthlyIncome(parseBigDecimal(dto.getMonthlyIncome(), line, "monthlyIncome"));
        profile.setNetWorth(parseBigDecimal(dto.getNetWorth(), line, "netWorth"));

        // Date
        profile.setAccountOpenedOn(parseLocalDate(dto.getAccountOpenedOn(), line, "accountOpenedOn"));

        // Optional fields (trimmed)
        profile.setIdType(safe(dto.getIdType()));
        profile.setIdNumber(safe(dto.getIdNumber()));
        profile.setNationality(safe(dto.getNationality()));
        profile.setCountryOfResidence(safe(dto.getCountryOfResidence()));

        profile.setRiskRating(dto.getRiskRating() != null ? dto.getRiskRating().trim() : "LOW");

        profile.setPep(parseBooleanStrict(dto.getIsPep(), line, "isPep"));
        profile.setDormant(parseBooleanStrict(dto.getIsDormant(), line, "isDormant"));

        return profile;
    }
}