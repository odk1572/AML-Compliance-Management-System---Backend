package com.app.aml.feature.ruleengine.repository;

import com.app.aml.feature.ruleengine.entity.GeographicRiskRating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeographicRiskRatingRepository extends JpaRepository<GeographicRiskRating, UUID> {

    Optional<GeographicRiskRating> findByCountryCodeAndSysIsDeletedFalse(String countryCode);

    Page<GeographicRiskRating> findAllBySysIsDeletedFalse(Pageable pageable);

    boolean existsByCountryCodeAndSysIsDeletedFalse(String countryCode);
}