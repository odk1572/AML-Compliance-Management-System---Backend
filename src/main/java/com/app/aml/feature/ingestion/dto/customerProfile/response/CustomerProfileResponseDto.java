package com.app.aml.feature.ingestion.dto.customerProfile.response;

import com.app.aml.domain.enums.CustomerType;
import com.app.aml.domain.enums.KycStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfileResponseDto {
    private UUID id;
    private String accountNumber;
    private String customerName;
    private CustomerType customerType;
    private String idType;
    private String idNumber;
    private String nationality;
    private String countryOfResidence;
    private BigDecimal monthlyIncome;
    private BigDecimal netWorth;
    private String riskRating;
    private Integer riskScore;
    private boolean isPep;
    private boolean isDormant;
    private LocalDate accountOpenedOn;
    private LocalDate lastActivityDate;
    private KycStatus kycStatus;
    private Instant sysCreatedAt;
    private Instant sysUpdatedAt;
}