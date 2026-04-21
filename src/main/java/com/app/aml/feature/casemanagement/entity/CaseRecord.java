package com.app.aml.feature.casemanagement.entity;

import com.app.aml.domain.enums.CasePriority;
import com.app.aml.domain.enums.CaseStatus;
import com.app.aml.domain.enums.ClosureDisposition;
import com.app.aml.shared.audit.SoftDeletableEntity;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cases")
@Getter
@Setter
@NoArgsConstructor
public class CaseRecord extends SoftDeletableEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @NotBlank
    @Size(max = 50)
    @Column(name = "case_reference", unique = true, nullable = false, length = 50)
    private String caseReference;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CaseStatus status = CaseStatus.OPEN;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private CasePriority priority = CasePriority.MEDIUM;

    @Column(name = "aggregated_risk_score")
    private Integer aggregatedRiskScore = 0;

    @NotNull
    @Column(name = "opened_at", nullable = false)
    private Instant openedAt = Instant.now();

    @NotNull
    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt = Instant.now();

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by")
    private UUID closedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "closure_disposition", length = 30)
    private ClosureDisposition closureDisposition;

    @Column(name = "false_positive_rationale", columnDefinition = "TEXT")
    private String falsePositiveRationale;

    @Column(name = "has_investigation_note", nullable = false)
    private boolean hasInvestigationNote = false;

    @Column(name = "sys_deleted_by")
    private UUID sysDeletedBy;
}