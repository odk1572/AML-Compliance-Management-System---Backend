package com.app.aml.feature.ruleengine.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Link entity connecting GlobalScenarios to GlobalRules.
 * Includes priority ordering and activation toggles.
 */
@Entity
@Table(
        name = "global_scenario_rules",
        schema = "common_schema",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_scenario_rule",
                columnNames = {"scenario_id", "rule_id"}
        )
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class GlobalScenarioRule {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @NotNull(message = "Scenario reference is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private GlobalScenario scenario;

    @NotNull(message = "Rule reference is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private GlobalRule rule;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "priority_order", nullable = false)
    private int priorityOrder = 0;

    /**
     * Audit field for creation time only.
     * UpdatedAt is excluded as per specific DB requirements for this link table.
     */
    @CreatedDate
    @Column(name = "sys_created_at", nullable = false, updatable = false)
    private LocalDateTime sysCreatedAt;
}