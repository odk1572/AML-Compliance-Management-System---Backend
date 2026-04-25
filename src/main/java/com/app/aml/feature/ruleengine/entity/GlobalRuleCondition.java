package com.app.aml.feature.ruleengine.entity;

import com.app.aml.shared.audit.AuditableEntity;
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
@Table(
        name = "global_rule_conditions",
        schema = "common_schema",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_grc_rule_sequence", columnNames = {"rule_id", "condition_sequence"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class GlobalRuleCondition extends AuditableEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @NotNull(message = "Rule mapping is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private GlobalRule rule;

    @NotBlank(message = "Aggregation function is required")
    @Size(max = 10, message = "Aggregation function cannot exceed 10 characters")
    @Column(name = "aggregation_function", nullable = false, length = 10)
    private String aggregationFunction = "NONE";

    @Size(max = 10, message = "Lookback period cannot exceed 10 characters")
    @Column(name = "lookback_period", length = 10)
    private String lookbackPeriod;

    @NotBlank(message = "Threshold value is required")
    @Size(max = 255, message = "Threshold value cannot exceed 255 characters")
    @Column(name = "threshold_value", nullable = false, length = 255)
    private String thresholdValue;

}