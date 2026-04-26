package com.app.aml.feature.ruleengine.entity;

import com.app.aml.domain.enums.AlertSeverity;
import com.app.aml.shared.audit.SoftDeletableEntity;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a specific AML Rule.
 * The ruleType field maps directly to the RuleExecutor Strategy identifiers.
 */
@Entity
@Table(name = "global_rules", schema = "common_schema")
@Getter
@Setter
@NoArgsConstructor
public class GlobalRule extends SoftDeletableEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @NotBlank(message = "Rule name is required")
    @Size(max = 150)
    @Column(name = "rule_name", nullable = false, length = 150)
    private String ruleName;

    /**
     * Maps to Strategy identifiers (e.g., "STRUCTURING", "VELOCITY").
     * Used by RuleExecutorFactory to route execution.
     */
    @NotBlank(message = "Rule type is required")
    @Size(max = 50)
    @Column(name = "rule_type", nullable = false, length = 50)
    private String ruleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 10)
    private AlertSeverity severity = AlertSeverity.MEDIUM;

    @Min(0)
    @Max(100)
    @Column(name = "base_risk_score", nullable = false)
    private int baseRiskScore = 50;

    public void softDelete() {
        this.setSysIsDeleted(true);
        this.setSysDeletedAt(Instant.now());
    }
}