package com.app.aml.feature.casemanagement.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "case_assignments")
@Getter
@Setter
@NoArgsConstructor
public class CaseAssignment {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private CaseRecord caseRecord;

    @Column(name = "assigned_from")
    private UUID assignedFrom;

    @NotNull
    @Column(name = "assigned_to", nullable = false)
    private UUID assignedTo;

    @NotNull
    @Column(name = "assigned_by", nullable = false)
    private UUID assignedBy;

    @Column(name = "assignment_reason", columnDefinition = "TEXT")
    private String assignmentReason;

    @NotNull
    @Column(name = "sys_created_at", nullable = false, updatable = false)
    private Instant sysCreatedAt = Instant.now();
}