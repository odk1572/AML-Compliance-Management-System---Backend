package com.app.aml.feature.ruleengine.entity;

import com.app.aml.shared.audit.AuditableEntity;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
        name = "tenant_rule_thresholds",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tenant_rule_condition", columnNames = {"tenant_rule_id", "global_condition_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class TenantRuleThreshold extends AuditableEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_rule_id", nullable = false)
    private TenantRule tenantRule;

    @NotNull
    @Column(name = "global_condition_id", nullable = false)
    private UUID globalConditionId;

    @Size(max = 255)
    @Column(name = "override_value", length = 255)
    private String overrideValue;

    @Size(max = 50)
    @Column(name = "override_lookback_period", length = 50)
    private String overrideLookbackPeriod;

    @Size(max = 50)
    @Column(name = "override_aggregation_function", length = 50)
    private String overrideAggregationFunction;
}