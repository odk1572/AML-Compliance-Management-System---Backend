package com.app.aml.feature.ingestion.entity;

import com.app.aml.domain.enums.AlertSeverity;
import com.app.aml.domain.enums.AlertStatus;
import com.app.aml.shared.audit.AuditableEntity;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "alerts")
@Getter
@Setter
@NoArgsConstructor
public class Alert extends AuditableEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    // The primary customer associated with the alert
    @NotNull
    @Column(name = "customer_profile_id", nullable = false)
    private UUID customerProfileId;

    // Direct reference to the prima
    // Change this to be optional
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggering_transaction_id", nullable = true) // Set to true
    private Transaction transaction;

    // Local Scenario Execution Context
    @NotNull
    @Column(name = "tenant_scenario_id", nullable = false)
    private UUID tenantScenarioId;

    // Cross-Schema IDs are usually kept as raw UUIDs in JPA multitenancy
    @NotNull
    @Column(name = "global_scenario_id", nullable = false)
    private UUID globalScenarioId;

    @NotNull
    @Column(name = "global_rule_id", nullable = false)
    private UUID globalRuleId;

    // Optional local rule reference
    @Column(name = "tenant_rule_id")
    private UUID tenantRuleId;

    @NotBlank
    @Size(max = 50)
    @Column(name = "alert_reference", unique = true, nullable = false, length = 50)
    private String alertReference;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private AlertSeverity severity;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private AlertStatus status = AlertStatus.NEW;

    @NotBlank
    @Size(max = 255)
    @Column(name = "typology_triggered", nullable = false, length = 255)
    private String typologyTriggered;

    @NotNull
    @Digits(integer = 3, fraction = 2)
    @Column(name = "risk_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal riskScore = BigDecimal.ZERO;
}