package com.app.aml.feature.strfiling.entity;

import com.app.aml.feature.ingestion.entity.Transaction;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "str_filing_transactions",
        schema = "common_schema",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_str_transaction", columnNames = {"str_filing_id", "transaction_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class StrFilingTransaction {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "str_filing_id", nullable = false)
    private StrFiling strFiling;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @NotNull
    @Column(name = "sys_created_at", nullable = false, updatable = false)
    private Instant sysCreatedAt = Instant.now();
}