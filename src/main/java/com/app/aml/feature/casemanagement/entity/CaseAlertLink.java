package com.app.aml.feature.casemanagement.entity;

import com.app.aml.feature.alert.entity.Alert;
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
        name = "case_alert_links",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_case_alert_link", columnNames = {"case_id", "alert_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class CaseAlertLink {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private CaseRecord caseRecord;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false)
    private Alert alert;

    @NotNull
    @Column(name = "linked_by", nullable = false)
    private UUID linkedBy;

    @NotNull
    @Column(name = "is_primary_alert", nullable = false)
    private boolean isPrimaryAlert = false;
    @NotNull
    @Column(name = "sys_created_at", nullable = false, updatable = false)
    private Instant sysCreatedAt = Instant.now();
}