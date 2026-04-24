package com.app.aml.feature.ruleengine.repository;

import com.app.aml.feature.ruleengine.entity.GlobalScenario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GlobalScenarioRepository extends JpaRepository<GlobalScenario, UUID> {
    boolean existsByScenarioNameAndSysIsDeletedFalse(String scenarioName);
    Optional<GlobalScenario> findByIdAndSysIsDeletedFalse(UUID id);
    Page<GlobalScenario> findAllBySysIsDeletedFalse(Pageable pageable);
    boolean existsByIdAndSysIsDeletedFalse(UUID scenarioId);
}