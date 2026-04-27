package com.app.aml.feature.casemanagement.entity;


import com.app.aml.feature.ingestion.entity.Transaction;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "case_transactions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_case_transaction_link",
                columnNames = {"case_id", "transaction_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class CaseTransaction {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private CaseRecord caseRecord;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @CreationTimestamp
    @Column(name = "sys_created_at", nullable = false, updatable = false)
    private Instant sysCreatedAt;
}