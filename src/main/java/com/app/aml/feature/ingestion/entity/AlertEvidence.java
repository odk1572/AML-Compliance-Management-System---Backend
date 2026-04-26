package com.app.aml.feature.ingestion.entity;

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
 * Entity representing the specific evidentiary values that triggered an alert.
 * This acts as an immutable snapshot of the rule logic at the moment of breach.
 */
@Entity
@Table(name = "alert_evidence")
@Getter
@Setter
@NoArgsConstructor
public class AlertEvidence {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @NotNull(message = "Alert reference is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false)
    private Alert alert;

    /**
     * The display name of the attribute, e.g., 'Single Transaction Limit'.
     */
    @NotBlank
    @Size(max = 100)
    @Column(name = "attribute_name", nullable = false, length = 100)
    private String attributeName;

    /**
     * The type of aggregation used, e.g., 'NONE', 'SUM', 'COUNT'.
     */
    @NotBlank
    @Size(max = 10)
    @Column(name = "aggregation_function", nullable = false, length = 10)
    private String aggregationFunction;

    /**
     * The logical operator applied, e.g., 'GREATER_THAN', 'EQUALS'.
     */
    @NotBlank
    @Size(max = 30)
    @Column(name = "operator", nullable = false, length = 30)
    private String operator;

    /**
     * The threshold value at the time of the breach (Global or Tenant override).
     */
    @NotBlank
    @Size(max = 255)
    @Column(name = "threshold_applied", nullable = false, length = 255)
    private String thresholdApplied;

    /**
     * The actual value found in the data that triggered the rule.
     */
    @NotBlank
    @Size(max = 255)
    @Column(name = "actual_evaluated_value", nullable = false, length = 255)
    private String actualEvaluatedValue;

    @NotNull
    @Column(name = "sys_created_at", nullable = false, updatable = false)
    private Instant sysCreatedAt = Instant.now();
}