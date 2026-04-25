package com.app.aml.feature.ingestion.batch.customer;

import com.app.aml.domain.enums.CustomerType;
import com.app.aml.feature.ingestion.batch.ValidationException;
import com.app.aml.feature.ingestion.entity.CustomerProfile;
import com.app.aml.feature.ingestion.repository.CustomerProfileRepository;
import com.app.aml.multitenency.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import static com.app.aml.feature.ingestion.batch.util.BatchValidationUtils.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerProfileValidationProcessor implements ItemProcessor<CustomerProfileCsvDto, CustomerProfile> {

    private final CustomerProfileRepository customerProfileRepository;

    private String tenantId;
    private String schemaName;

    /**
     * Captures the tenant info from the main thread before worker threads take over.
     */
    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.tenantId = stepExecution.getJobParameters().getString("tenantId");
        this.schemaName = stepExecution.getJobParameters().getString("schemaName");
    }

    @Override
    public CustomerProfile process(CustomerProfileCsvDto dto) {
        // --- CRITICAL FIX FOR MULTI-THREADING ---
        // Force the worker thread into the correct tenant/schema context
        if (TenantContext.getSchemaName() == null) {
            TenantContext.setTenantId(tenantId);
            TenantContext.setSchemaName(schemaName);
        }

        int line = dto.getLineNumber();

        // 1. Account Number & Idempotency Check
        String accNum = require(dto.getAccountNumber(), line, "accountNumber");

        if (customerProfileRepository.existsByAccountNumber(accNum)) {
            log.debug("Skipping existing customer at line {}: {}", line, accNum);
            return null;
        }

        CustomerProfile profile = new CustomerProfile();
        profile.setAccountNumber(accNum);

        // 2. Name and Identity
        profile.setCustomerName(require(dto.getCustomerName(), line, "customerName"));
        profile.setIdType(safe(dto.getIdType()));
        profile.setIdNumber(safe(dto.getIdNumber()));

        // 3. Customer Type (Enum)
        String typeRaw = dto.getCustomerType();
        if (typeRaw != null && !typeRaw.isBlank()) {
            try {
                profile.setCustomerType(CustomerType.valueOf(typeRaw.trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new ValidationException(line, "customerType", "Invalid value: " + typeRaw);
            }
        } else {
            throw new ValidationException(line, "customerType", "Customer type is required");
        }

        // 4. Financials
        profile.setMonthlyIncome(parseBigDecimal(dto.getMonthlyIncome(), line, "monthlyIncome"));
        profile.setNetWorth(parseBigDecimal(dto.getNetWorth(), line, "netWorth"));

        // 5. Date Parsing
        profile.setAccountOpenedOn(parseLocalDate(dto.getAccountOpenedOn(), line, "accountOpenedOn"));

        // 6. Demographics
        profile.setNationality(safe(dto.getNationality()));
        profile.setCountryOfResidence(safe(dto.getCountryOfResidence()));

        // 7. Risk Rating & Risk Score (New from your CSV)
        String risk = dto.getRiskRating();
        profile.setRiskRating(risk != null && !risk.isBlank() ? risk.trim().toUpperCase() : "LOW");

        if (dto.getRiskScore() != null && !dto.getRiskScore().isBlank()) {
            try {
                profile.setRiskScore(Integer.parseInt(dto.getRiskScore().trim()));
            } catch (NumberFormatException e) {
                profile.setRiskScore(0);
            }
        }

        // 8. Boolean Flags
        profile.setPep(parseBooleanStrict(dto.getIsPep(), line, "isPep"));
        profile.setDormant(parseBooleanStrict(dto.getIsDormant(), line, "isDormant"));

        // 9. Last Activity Date (New from your CSV)
        if (dto.getLastActivityDate() != null && !dto.getLastActivityDate().isBlank()) {
            profile.setLastActivityDate(parseLocalDate(dto.getLastActivityDate(), line, "lastActivityDate"));
        }

        return profile;
    }
}