package com.app.aml.feature.ruleengine.entity;

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

/**
 * Merged entity representing a tenant's specific instance and configuration of a Global Rule.
 * Lives in the tenant-specific schema.
 */
@Entity
@Table(name = "tenant_rules")
@Getter
@Setter
@NoArgsConstructor
public class TenantRule extends SoftDeletableEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    /**
     * Reference to the activated scenario this rule belongs to.
     */
    @NotNull(message = "Tenant scenario reference is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_scenario_id", nullable = false)
    private TenantScenario tenantScenario;

    /**
     * Cross-schema reference to the GlobalRule ID in common_schema.
     */
    @NotNull(message = "Global rule reference is required")
    @Column(name = "global_rule_id", nullable = false)
    private UUID globalRuleId;

    /**
     * Custom bank-specific code for the rule (e.g., "BA-STR-001").
     */
    @NotBlank
    @Size(max = 50)
    @Column(name = "rule_code", nullable = false, length = 50)
    private String ruleCode;

    /**
     * Custom bank-specific label for the rule.
     */
    @NotBlank
    @Size(max = 255)
    @Column(name = "rule_name", nullable = false, length = 255)
    private String ruleName;

    /**
     * Granular toggle to enable/disable this specific rule within the scenario.
     */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "sys_created_by")
    private UUID sysCreatedBy;

    // ============================================================
    // Business Logic Methods
    // ============================================================

    /**
     * Toggles the active status of the rule.
     * @param active desired state.
     */
    public void toggle(boolean active) {
        this.isActive = active;
    }

    /**
     * Performs a soft delete of the tenant-specific rule configuration.
     */
    public void softDelete() {
        this.setSysIsDeleted(true);
        this.setSysDeletedAt(Instant.now());
    }
}