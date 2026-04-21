package com.app.aml.feature.ruleengine.entity;

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
@Table(name = "tenant_rules")
@Getter
@Setter
@NoArgsConstructor
public class TenantRule extends SoftDeletableEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_scenario_id", nullable = false)
    private TenantScenario tenantScenario;

    @NotNull
    @Column(name = "global_rule_id", nullable = false)
    private UUID globalRuleId;

    @NotBlank
    @Size(max = 100)
    @Column(name = "rule_code", unique = true, nullable = false, length = 100)
    private String ruleCode;

    @NotBlank
    @Size(max = 255)
    @Column(name = "rule_name", nullable = false, length = 255)
    private String ruleName;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "sys_created_by")
    private UUID sysCreatedBy;
}