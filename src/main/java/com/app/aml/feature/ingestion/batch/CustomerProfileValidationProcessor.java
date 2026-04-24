package com.app.aml.feature.ingestion.batch;

import com.app.aml.domain.enums.CustomerType;
import com.app.aml.feature.ingestion.entity.CustomerProfile;
import com.app.aml.feature.ingestion.repository.CustomerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
@Component
@RequiredArgsConstructor
public class CustomerProfileValidationProcessor implements ItemProcessor<CustomerProfileCsvDto, CustomerProfile> {

    private final CustomerProfileRepository customerProfileRepository;

    @Override
    public CustomerProfile process(CustomerProfileCsvDto dto) {

        int line = dto.getLineNumber();
        CustomerProfile profile = new CustomerProfile();

        // Account Number
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
        profile.setAccountOpenedOn(parseDate(dto.getAccountOpenedOn(), line, "accountOpenedOn"));

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

    // ===== helpers =====

    private String require(String value, int line, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(line, field, "Cannot be null or blank");
        }
        return value.trim();
    }

    private String safe(String value) {
        return value == null ? null : value.trim();
    }

    private BigDecimal parseBigDecimal(String value, int line, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(line, field, "Cannot be null or blank");
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException(line, field, "Invalid decimal: " + value);
        }
    }

    private LocalDate parseDate(String value, int line, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(line, field, "Cannot be null");
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            throw new ValidationException(line, field, "Invalid date format (yyyy-MM-dd): " + value);
        }
    }

    private Boolean parseBooleanStrict(String value, int line, String field) {
        if (value == null) return false;

        String v = value.trim().toLowerCase();

        if (v.equals("true") || v.equals("1") || v.equals("yes")) return true;
        if (v.equals("false") || v.equals("0") || v.equals("no")) return false;

        throw new ValidationException(line, field, "Invalid boolean: " + value);
    }
}