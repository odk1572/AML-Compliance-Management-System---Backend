package com.app.aml.feature.ruleengine.entity;

import com.app.aml.enums.RuleStatus;
import com.app.aml.audit.AuditableEntity;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;


@Entity
@Table(
        name = "tenant_scenarios",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tenant_global_scenario",
                columnNames = {"global_scenario_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class TenantScenario extends AuditableEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();


    @NotNull(message = "Global scenario reference is required")
    @Column(name = "global_scenario_id", nullable = false)
    private UUID globalScenarioId;

    @NotNull(message = "Status cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RuleStatus status = RuleStatus.PAUSED;

    @Column(name = "sys_activated_by")
    private UUID sysActivatedBy;

    public void activate(UUID adminId) {
        this.status = RuleStatus.ACTIVE;
        this.sysActivatedBy = adminId;
    }

    public void pause() {
        this.status = RuleStatus.PAUSED;
    }
}