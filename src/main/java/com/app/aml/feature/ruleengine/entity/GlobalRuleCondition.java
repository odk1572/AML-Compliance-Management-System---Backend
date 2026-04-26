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

/**
 * Entity representing a specific parameter/condition for a Global Rule.
 * The aggregationFunction acts as a semantic tag for Rule Executors.
 */
@Entity
@Table(name = "global_rule_conditions", schema = "common_schema")
@Getter
@Setter
@NoArgsConstructor
public class GlobalRuleCondition extends AuditableEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @NotNull(message = "Rule association is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private GlobalRule rule;

    /**
     * UI display label (e.g., "Single Transaction Limit").
     * No strict CHECK constraint; used for frontend rendering.
     */
    @NotBlank(message = "Attribute name is required")
    @Size(max = 100)
    @Column(name = "attribute_name", nullable = false, length = 100)
    private String attributeName;

    /**
     * Semantic parameter tag: NONE, SUM, COUNT, AVG, MIN, MAX.
     * CRITICAL: Executors switch on this to map data to logic parameters.
     */
    @NotBlank(message = "Aggregation function tag is required")
    @Size(max = 10)
    @Column(name = "aggregation_function", nullable = false, length = 10)
    private String aggregationFunction;

    /**
     * Time window for the condition (e.g., "24h", "30d").
     * Nullable for conditions that are pure value checks.
     */
    @Size(max = 10)
    @Column(name = "lookback_period", length = 10)
    private String lookbackPeriod;

    /**
     * The default threshold value (e.g., "10000", "5").
     */
    @NotBlank(message = "Threshold value is required")
    @Size(max = 255)
    @Column(name = "threshold_value", nullable = false, length = 255)
    private String thresholdValue;

    /**
     * Metadata describing the data type (e.g., "NUMERIC", "STRING", "DECIMAL").
     */
    @NotBlank(message = "Value data type is required")
    @Size(max = 50)
    @Column(name = "value_data_type", nullable = false, length = 50)
    private String valueDataType;
}