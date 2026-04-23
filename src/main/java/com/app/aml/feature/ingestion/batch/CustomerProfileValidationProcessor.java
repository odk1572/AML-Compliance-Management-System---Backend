package com.app.aml.feature.ingestion.batch;

import com.app.aml.domain.enums.CustomerType;
import com.app.aml.feature.ingestion.entity.CustomerProfile;
import org.springframework.stereotype.Component;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Component
public class CustomerProfileValidationProcessor implements ItemProcessor<CustomerProfileCsvDto, CustomerProfile> {

    @Override
    public CustomerProfile process(CustomerProfileCsvDto dto) throws ValidationException {
        CustomerProfile profile = new CustomerProfile();

        try {
            // Validate Account Number (No Null)
            if (dto.getAccountNumber() == null || dto.getAccountNumber().isBlank()) {
                throw new ValidationException(dto.getLineNumber(), "accountNumber", "Cannot be null or blank");
            }
            profile.setAccountNumber(dto.getAccountNumber());

            // Validate Customer Name
            if (dto.getCustomerName() == null || dto.getCustomerName().isBlank()) {
                throw new ValidationException(dto.getLineNumber(), "customerName", "Cannot be null or blank");
            }
            profile.setCustomerName(dto.getCustomerName());

            // Validate Type Matching (Enum)
            try {
                profile.setCustomerType(CustomerType.valueOf(dto.getCustomerType()));
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new ValidationException(dto.getLineNumber(), "customerType", "Invalid or missing Customer Type");
            }

            // Validate Type Matching (BigDecimal)
            try {
                profile.setMonthlyIncome(dto.getMonthlyIncome() != null ? new BigDecimal(dto.getMonthlyIncome()) : BigDecimal.ZERO);
                profile.setNetWorth(dto.getNetWorth() != null ? new BigDecimal(dto.getNetWorth()) : BigDecimal.ZERO);
            } catch (NumberFormatException e) {
                throw new ValidationException(dto.getLineNumber(), "monthlyIncome/netWorth", "Must be a valid decimal number");
            }

            // Validate Dates
            try {
                if (dto.getAccountOpenedOn() == null) throw new NullPointerException();
                profile.setAccountOpenedOn(LocalDate.parse(dto.getAccountOpenedOn()));
            } catch (DateTimeParseException | NullPointerException e) {
                throw new ValidationException(dto.getLineNumber(), "accountOpenedOn", "Invalid date format. Expected YYYY-MM-DD");
            }

            // Map standard text fields safely
            profile.setIdType(dto.getIdType());
            profile.setIdNumber(dto.getIdNumber());
            profile.setNationality(dto.getNationality());
            profile.setCountryOfResidence(dto.getCountryOfResidence());
            profile.setRiskRating(dto.getRiskRating() != null ? dto.getRiskRating() : "LOW");
            profile.setPep(Boolean.parseBoolean(dto.getIsPep()));
            profile.setDormant(Boolean.parseBoolean(dto.getIsDormant()));

            return profile;

        } catch (ValidationException e) {
            throw e;
        }
    }
}