package com.app.aml.feature.ruleengine.entity;

import com.app.aml.domain.enums.AlertSeverity;
import com.app.aml.shared.audit.SoftDeletableEntity;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

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
    @Size(max = 150, message = "Rule name cannot exceed 150 characters")
    @Column(name = "rule_name", nullable = false, length = 150)
    private String ruleName;

    @NotBlank(message = "Rule type is required")
    @Size(max = 50, message = "Rule type cannot exceed 50 characters")
    @Column(name = "rule_type", nullable = false, length = 50)
    private String ruleType;

    @NotNull(message = "Severity is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 10)
    private AlertSeverity severity;

    @NotNull(message = "Base risk score is required")
    @Column(name = "base_risk_score", nullable = false)
    private Short baseRiskScore = 50;
}