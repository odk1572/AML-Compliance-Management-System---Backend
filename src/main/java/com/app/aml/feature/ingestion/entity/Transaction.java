package com.app.aml.feature.ingestion.entity;

import com.app.aml.enums.Channel;
import com.app.aml.enums.TransactionStatus;
import com.app.aml.enums.TransactionType;
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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
public class Transaction {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private TransactionBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private CustomerProfile customer;

    @NotBlank
    @Size(max = 100)
    @Column(name = "transaction_ref", unique = true, nullable = false, length = 100)
    private String transactionRef;

    @Size(max = 50)
    @Column(name = "originator_account_no", length = 50)
    private String originatorAccountNo;

    @Size(max = 255)
    @Column(name = "originator_name", length = 255)
    private String originatorName;

    @Size(max = 50)
    @Column(name = "originator_bank_code", length = 50)
    private String originatorBankCode;

    @Size(max = 3)
    @Column(name = "originator_country", length = 3)
    private String originatorCountry;

    @Size(max = 50)
    @Column(name = "beneficiary_account_no", length = 50)
    private String beneficiaryAccountNo;

    @Size(max = 255)
    @Column(name = "beneficiary_name", length = 255)
    private String beneficiaryName;

    @Size(max = 50)
    @Column(name = "beneficiary_bank_code", length = 50)
    private String beneficiaryBankCode;

    @Size(max = 3)
    @Column(name = "beneficiary_country", length = 3)
    private String beneficiaryCountry;

    @NotNull
    @Digits(integer = 18, fraction = 2)
    @Column(name = "amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal amount;

    @NotBlank
    @Size(max = 3)
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private Channel channel;

    @NotNull
    @Column(name = "transaction_timestamp", nullable = false)
    private Instant transactionTimestamp;

    @Column(name = "reference_note", columnDefinition = "TEXT")
    private String referenceNote;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.CLEAN;

    @NotNull
    @Column(name = "sys_created_at", nullable = false, updatable = false)
    private Instant sysCreatedAt = Instant.now();
}