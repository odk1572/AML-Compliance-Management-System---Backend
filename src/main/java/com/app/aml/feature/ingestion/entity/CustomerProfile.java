package com.app.aml.feature.ingestion.entity;

import com.app.aml.enums.CustomerType;
import com.app.aml.enums.KycStatus;
import com.app.aml.shared.audit.SoftDeletableEntity;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "customer_profiles")
@Getter
@Setter
@NoArgsConstructor
public class CustomerProfile extends SoftDeletableEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @NotBlank
    @Size(max = 50)
    @Column(name = "account_number", unique = true, nullable = false, length = 50)
    private String accountNumber;

    @NotBlank
    @Size(max = 255)
    @Column(name = "customer_name", nullable = false, length = 255)
    private String customerName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false, length = 20)
    private CustomerType customerType;

    @Size(max = 50)
    @Column(name = "id_type", length = 50)
    private String idType;

    @Size(max = 100)
    @Column(name = "id_number", length = 100)
    private String idNumber;

    @Size(max = 3)
    @Column(name = "nationality", length = 3)
    private String nationality;

    @Size(max = 3)
    @Column(name = "country_of_residence", length = 3)
    private String countryOfResidence;

    @Digits(integer = 18, fraction = 2)
    @Column(name = "monthly_income", precision = 20, scale = 2)
    private BigDecimal monthlyIncome = BigDecimal.ZERO;
    @Digits(integer = 18, fraction = 2)
    @Column(name = "net_worth", precision = 20, scale = 2)
    private BigDecimal netWorth = BigDecimal.ZERO;

    @NotBlank
    @Size(max = 20)
    @Column(name = "risk_rating", nullable = false, length = 20)
    private String riskRating = "LOW";

    @Column(name = "risk_score")
    private Integer riskScore = 0;

    @Column(name = "is_pep", nullable = false)
    private boolean isPep = false;

    @Column(name = "is_dormant", nullable = false)
    private boolean isDormant = false;

    @NotNull
    @Column(name = "account_opened_on", nullable = false)
    private LocalDate accountOpenedOn;

    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 20)
    private KycStatus kycStatus = KycStatus.PENDING;
}