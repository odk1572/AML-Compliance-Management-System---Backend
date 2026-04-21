package com.app.aml.feature.casemanagement.entity;

import com.app.aml.domain.enums.NoteType;
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
@Table(name = "case_notes")
@Getter
@Setter
@NoArgsConstructor
public class CaseNote {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private CaseRecord caseRecord;

    @NotNull
    @Column(name = "authored_by", nullable = false)
    private UUID authoredBy;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "note_type", nullable = false, length = 50)
    private NoteType noteType;

    @NotBlank
    @Column(name = "note_content", nullable = false, columnDefinition = "TEXT")
    private String noteContent;

    @NotNull
    @Column(name = "sys_created_at", nullable = false, updatable = false)
    private Instant sysCreatedAt = Instant.now();
}