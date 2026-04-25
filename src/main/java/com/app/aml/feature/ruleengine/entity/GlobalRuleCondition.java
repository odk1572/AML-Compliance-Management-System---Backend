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

    @NotNull(message = "Rule mapping is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private GlobalRule rule;

    @NotBlank(message = "Attribute name is required")
    @Size(max = 100, message = "Attribute name cannot exceed 100 characters")
    @Column(name = "attribute_name", nullable = false, length = 100)
    private String attributeName;

    @NotBlank(message = "Threshold value is required")
    @Size(max = 255, message = "Threshold value cannot exceed 255 characters")
    @Column(name = "threshold_value", nullable = false, length = 255)
    private String thresholdValue;

    @NotBlank(message = "Value data type is required")
    @Size(max = 50, message = "Value data type cannot exceed 50 characters")
    @Column(name = "value_data_type", nullable = false, length = 50)
    private String valueDataType;
}