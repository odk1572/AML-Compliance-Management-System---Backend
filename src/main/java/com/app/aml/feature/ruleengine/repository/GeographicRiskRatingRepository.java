package com.app.aml.feature.ruleengine.repository;

import com.app.aml.feature.ruleengine.entity.GeographicRiskRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface GeographicRiskRatingRepository extends JpaRepository<GeographicRiskRating, UUID> {
}