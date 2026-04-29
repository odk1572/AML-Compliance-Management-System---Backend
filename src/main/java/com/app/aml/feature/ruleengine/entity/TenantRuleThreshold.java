package com.app.aml.feature.ruleengine.entity;

import com.app.aml.UX.ReferenceGenerator;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Entity for tenant-specific overrides of global rule conditions.
 * If an override field is NULL, the system defaults to the GlobalRuleCondition value.
 * This entity does not carry its own audit fields as it is managed as part of the TenantRule.
 */
@Entity
@Table(
        name = "tenant_rule_thresholds",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tenant_rule_condition",
                columnNames = {"tenant_rule_id", "global_condition_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class TenantRuleThreshold {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @Column(name = "threshold_code", unique = true, nullable = false, updatable = false, length = 50)
    private String thresholdCode;

    @NotNull(message = "Tenant rule reference is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_rule_id", nullable = false)
    private TenantRule tenantRule;

    @NotNull(message = "Global condition reference is required")
    @Column(name = "global_condition_id", nullable = false)
    private UUID globalConditionId;

    @Size(max = 255)
    @Column(name = "override_value")
    private String overrideValue;

    @Size(max = 10)
    @Column(name = "override_lookback_period", length = 10)
    private String overrideLookbackPeriod;

    @Size(max = 10)
    @Column(name = "override_aggregation_function", length = 10)
    private String overrideAggregationFunction;


    @PrePersist
    public void prePersistActions() {
        if (this.thresholdCode == null || this.thresholdCode.isBlank()) {
            this.thresholdCode = ReferenceGenerator.generate("TRT");
        }
    }
}