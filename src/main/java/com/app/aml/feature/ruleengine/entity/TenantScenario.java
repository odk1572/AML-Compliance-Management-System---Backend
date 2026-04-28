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

/**
 * Entity representing a tenant's activation of a Global Scenario.
 * This lives in the tenant-specific schema.
 */
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

    /**
     * Reference to the GlobalScenario ID in common_schema.
     * No physical Foreign Key is used to maintain cross-schema isolation.
     */
    @NotNull(message = "Global scenario reference is required")
    @Column(name = "global_scenario_id", nullable = false)
    private UUID globalScenarioId;

    /**
     * Current status of the scenario for this tenant.
     * Allowed values: ACTIVE, PAUSED.
     */
    @NotNull(message = "Status cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RuleStatus status = RuleStatus.PAUSED;

    /**
     * Audit field identifying the bank admin who last activated this scenario.
     */
    @Column(name = "sys_activated_by")
    private UUID sysActivatedBy;

    // ============================================================
    // Business Logic Methods
    // ============================================================

    /**
     * Activates the scenario for transaction monitoring.
     * @param adminId The ID of the admin performing the activation.
     */
    public void activate(UUID adminId) {
        this.status = RuleStatus.ACTIVE;
        this.sysActivatedBy = adminId;
    }

    /**
     * Pauses the scenario. Monitoring for this scenario will cease
     * until reactivated.
     */
    public void pause() {
        this.status = RuleStatus.PAUSED;
    }
}