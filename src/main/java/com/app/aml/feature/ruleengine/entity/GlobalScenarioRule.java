package com.app.aml.feature.ruleengine.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "global_scenario_rules",
        schema = "common_schema",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_scenario_rule", columnNames = {"scenario_id", "rule_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class GlobalScenarioRule {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private GlobalScenario scenario;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private GlobalRule rule;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @NotNull
    @Column(name = "priority_order", nullable = false)
    private Integer priorityOrder = 0;

    @NotNull
    @Column(name = "sys_created_at", nullable = false, updatable = false)
    private Instant sysCreatedAt = Instant.now();
}