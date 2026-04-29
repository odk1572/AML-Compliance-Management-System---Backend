package com.app.aml.feature.ruleengine.entity;

import com.app.aml.UX.ReferenceGenerator;
import com.app.aml.audit.SoftDeletableEntity;
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
@Table(name = "tenant_rules")
@Getter
@Setter
@NoArgsConstructor
public class TenantRule extends SoftDeletableEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();


    @NotNull(message = "Tenant scenario reference is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_scenario_id", nullable = false)
    private TenantScenario tenantScenario;

    @NotNull(message = "Global rule reference is required")
    @Column(name = "global_rule_id", nullable = false)
    private UUID globalRuleId;

    @Column(name = "rule_code", unique = true, nullable = false, updatable = false, length = 50)
    private String ruleCode;

    @NotBlank(message = "Rule name cannot be blank")
    @Size(max = 255)
    @Column(name = "rule_name", nullable = false, length = 255)
    private String ruleName;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "sys_created_by")
    private UUID sysCreatedBy;


    @PrePersist
    public void prePersistActions() {
        if (this.ruleCode == null || this.ruleCode.isBlank()) {
            // "TRL" = Tenant Rule
            this.ruleCode = ReferenceGenerator.generate("TRL");
        }
    }


    public void toggle(boolean active) {
        this.isActive = active;
    }

    public void softDelete() {
        this.setSysIsDeleted(true);
        this.setSysDeletedAt(Instant.now());
    }
}