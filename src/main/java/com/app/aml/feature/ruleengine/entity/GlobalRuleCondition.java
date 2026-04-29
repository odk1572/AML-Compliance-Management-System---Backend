package com.app.aml.feature.ruleengine.entity;

import com.app.aml.UX.ReferenceGenerator;
import com.app.aml.audit.AuditableEntity;
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

    @Column(name = "condition_reference", unique = true, nullable = false, updatable = false, length = 50)
    private String conditionCode;

    @NotNull(message = "Rule association is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private GlobalRule rule;

    @NotBlank(message = "Attribute name is required")
    @Size(max = 100)
    @Column(name = "attribute_name", nullable = false, length = 100)
    private String attributeName;

    @NotBlank(message = "Aggregation function tag is required")
    @Size(max = 10)
    @Column(name = "aggregation_function", nullable = false, length = 10)
    private String aggregationFunction;

    @Size(max = 10)
    @Column(name = "lookback_period", length = 10)
    private String lookbackPeriod;

    @NotBlank(message = "Threshold value is required")
    @Size(max = 255)
    @Column(name = "threshold_value", nullable = false, length = 255)
    private String thresholdValue;

    @NotBlank(message = "Value data type is required")
    @Size(max = 50)
    @Column(name = "value_data_type", nullable = false, length = 50)
    private String valueDataType;

    @PrePersist
    public void prePersistActions() {
        if (this.conditionCode == null || this.conditionCode.isBlank()) {
            this.conditionCode = ReferenceGenerator.generate("GRC");
        }
    }
}