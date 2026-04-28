package com.app.aml.feature.strfiling.entity;

import com.app.aml.feature.casemanagement.entity.CaseRecord;
import com.app.aml.feature.ingestion.entity.CustomerProfile;
import com.app.aml.feature.ingestion.entity.Transaction;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
    
    @NotBlank
    @Size(max = 100)
    @Column(name = "rule_type", nullable = false, length = 100)
    private String ruleType;

    @NotBlank
    @Size(max = 255)
    @Column(name = "typology_triggered", nullable = false, length = 255)
    private String typologyTriggered;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_profile_id", nullable = false)
    private CustomerProfile customer;

    @OneToMany(
            mappedBy = "strFiling",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<StrFilingTransaction> strTransactions = new ArrayList<>();

    @NotBlank
    @Column(name = "suspicion_narrative", nullable = false, columnDefinition = "TEXT")
    private String suspicionNarrative;

    @NotNull
    @Column(name = "filed_by", nullable = false)
    private UUID filedBy;

    @NotNull
    @Column(name = "sys_created_at", nullable = false, updatable = false)
    private Instant sysCreatedAt = Instant.now();

    public void addTransaction(Transaction transaction) {
        StrFilingTransaction strTxn = new StrFilingTransaction();
        strTxn.setStrFiling(this);
        strTxn.setTransaction(transaction);
        this.strTransactions.add(strTxn);
    }
}