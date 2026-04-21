package com.app.aml.feature.ruleengine.entity;

import com.app.aml.shared.audit.SoftDeletableEntity;
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

@Entity
@Table(name = "geographic_risk_ratings", schema = "common_schema")
@Getter
@Setter
@NoArgsConstructor
public class GeographicRiskRating extends SoftDeletableEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @NotBlank
    @Size(min = 2, max = 3)
    @Column(name = "country_code", unique = true, nullable = false, length = 3)
    private String countryCode;

    @NotBlank
    @Size(max = 255)
    @Column(name = "country_name", nullable = false, length = 255)
    private String countryName;

    @NotBlank
    @Size(max = 50)
    @Column(name = "fatf_status", nullable = false, length = 50)
    private String fatfStatus = "COMPLIANT";

    @NotNull
    @Column(name = "basel_aml_index_score", nullable = false)
    private Integer baselAmlIndexScore = 0;

    @NotBlank
    @Size(max = 50)
    @Column(name = "risk_tier", nullable = false, length = 50)
    private String riskTier = "LOW";

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @NotNull
    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom = Instant.now();
}