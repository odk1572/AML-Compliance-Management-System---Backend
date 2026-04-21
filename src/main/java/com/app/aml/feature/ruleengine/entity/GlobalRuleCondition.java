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
@Table(name = "global_rule_conditions", schema = "common_schema")
@Getter
@Setter
@NoArgsConstructor
public class GlobalRuleCondition extends AuditableEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private GlobalRule rule;

    @NotBlank
    @Size(max = 100)
    @Column(name = "attribute_name", nullable = false, length = 100)
    private String attributeName;

    @NotNull
    @Column(name = "condition_sequence", nullable = false)
    private Integer conditionSequence;

    @NotBlank
    @Size(max = 50)
    @Column(name = "aggregation_function", nullable = false, length = 50)
    private String aggregationFunction = "NONE";

    @Size(max = 50)
    @Column(name = "lookback_period", length = 50)
    private String lookbackPeriod;

    @NotBlank
    @Size(max = 50)
    @Column(name = "operator", nullable = false, length = 50)
    private String operator;

    @NotBlank
    @Size(max = 255)
    @Column(name = "threshold_value", nullable = false, length = 255)
    private String thresholdValue;

    @NotBlank
    @Size(max = 50)
    @Column(name = "value_data_type", nullable = false, length = 50)
    private String valueDataType;
}