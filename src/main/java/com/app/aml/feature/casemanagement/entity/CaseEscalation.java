package com.app.aml.feature.casemanagement.entity;

import com.app.aml.enums.EscalationStatus;
import com.app.aml.audit.AuditableEntity;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "case_escalations")
@Getter
@Setter
@NoArgsConstructor
public class CaseEscalation extends AuditableEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private CaseRecord caseRecord;

    @NotNull
    @Column(name = "escalated_by", nullable = false)
    private UUID escalatedBy;

    @NotNull
    @Column(name = "escalated_to", nullable = false)
    private UUID escalatedTo;

    @NotBlank
    @Column(name = "escalation_reason", nullable = false, columnDefinition = "TEXT")
    private String escalationReason;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "escalation_status", nullable = false, length = 30)
    private EscalationStatus escalationStatus = EscalationStatus.PENDING;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

}