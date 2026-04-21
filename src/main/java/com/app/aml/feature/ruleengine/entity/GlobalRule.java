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

    @NotBlank
    @Size(max = 255)
    @Column(name = "rule_name", nullable = false, length = 255)
    private String ruleName;

    @NotBlank
    @Size(max = 255)
    @Column(name = "condition_logic", nullable = false, length = 255)
    private String conditionLogic = "AND";

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 50)
    private AlertSeverity severity;

    @NotNull
    @Column(name = "base_risk_score", nullable = false)
    private Integer baseRiskScore = 0;
}