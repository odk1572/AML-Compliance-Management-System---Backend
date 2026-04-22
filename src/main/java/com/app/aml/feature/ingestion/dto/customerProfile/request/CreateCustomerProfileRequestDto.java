package com.app.aml.feature.ingestion.dto.customerProfile.request;

import com.app.aml.domain.enums.CustomerType;
import com.app.aml.domain.enums.KycStatus;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomerProfileRequestDto {

    @NotBlank(message = "Account number is required")
    @Size(max = 50, message = "Account number cannot exceed 50 characters")
    private String accountNumber;

    @NotBlank(message = "Customer name is required")
    @Size(max = 255)
    private String customerName;

    @NotNull(message = "Customer type is required")
    private CustomerType customerType;

    @Size(max = 50)
    private String idType;

    @Size(max = 100)
    private String idNumber;

    @Size(max = 3, message = "Nationality must be a 3-letter ISO code")
    private String nationality;

    @Size(max = 3, message = "Country of residence must be a 3-letter ISO code")
    private String countryOfResidence;

    @Digits(integer = 18, fraction = 2)
    private BigDecimal monthlyIncome;

    @Digits(integer = 18, fraction = 2)
    private BigDecimal netWorth;

    @Size(max = 20)
    private String riskRating; // Can be omitted to rely on entity default "LOW"

    private Integer riskScore;

    private Boolean isPep;

    private Boolean isDormant;

    @NotNull(message = "Account opening date is required")
    private LocalDate accountOpenedOn;

    private LocalDate lastActivityDate;

    private KycStatus kycStatus; // Can be omitted to rely on entity default "PENDING"
}