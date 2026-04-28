package com.app.aml.feature.ruleengine.entity;

import com.app.aml.audit.SoftDeletableEntity;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name = "global_scenarios", schema = "common_schema")
@Getter
@Setter
@NoArgsConstructor
public class GlobalScenario extends SoftDeletableEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @NotBlank(message = "Scenario name is required")
    @Size(max = 255, message = "Scenario name cannot exceed 255 characters")
    @Column(name = "scenario_name", unique = true, nullable = false, length = 255)
    private String scenarioName;

    @NotBlank(message = "Category is required")
    @Size(max = 100, message = "Category cannot exceed 100 characters")
    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_by")
    private UUID createdBy;


    public void softDelete() {
        this.setSysIsDeleted(true);
        this.setSysDeletedAt(Instant.now());
    }
}