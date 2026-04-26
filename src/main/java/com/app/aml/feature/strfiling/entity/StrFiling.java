package com.app.aml.feature.strfiling.entity;

import com.app.aml.domain.enums.TypologyCategory;
import com.app.aml.feature.casemanagement.entity.CaseRecord;
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
@Table(name = "str_filings")
@Getter
@Setter
@NoArgsConstructor
public class StrFiling {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private CaseRecord caseRecord;

    @NotBlank
    @Size(max = 50)
    @Column(name = "filing_reference", unique = true, nullable = false, length = 50)
    private String filingReference;

    @NotBlank
    @Size(max = 50)
    @Column(name = "regulatory_body", nullable = false, length = 50)
    private String regulatoryBody;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "typology_category", nullable = false, length = 100)
    private TypologyCategory typologyCategory;

    @NotBlank
    @Size(max = 255)
    @Column(name = "subject_name", nullable = false, length = 255)
    private String subjectName;

    @NotBlank
    @Size(max = 50)
    @Column(name = "subject_account_no", nullable = false, length = 50)
    private String subjectAccountNo;

    @NotBlank
    @Column(name = "suspicion_narrative", nullable = false, columnDefinition = "TEXT")
    private String suspicionNarrative;

    @NotNull
    @Column(name = "filed_by", nullable = false)
    private UUID filedBy;

    @NotNull
    @Column(name = "sys_created_at", nullable = false, updatable = false)
    private Instant sysCreatedAt = Instant.now();
}