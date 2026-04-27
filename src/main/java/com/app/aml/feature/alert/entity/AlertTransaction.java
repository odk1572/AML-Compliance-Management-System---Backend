package com.app.aml.feature.alert.entity;


import com.app.aml.enums.InvolvementRole;
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
        name = "alert_transactions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_alert_transaction_link",
                columnNames = {"alert_id", "transaction_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class AlertTransaction {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false)
    private Alert alert;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "involvement_role", length = 50)
    private InvolvementRole involvementRole = InvolvementRole.CONTRIBUTOR;

    @CreationTimestamp
    @Column(name = "sys_created_at", nullable = false, updatable = false)
    private Instant sysCreatedAt;
}