package com.app.aml.feature.ruleengine.repository;

import com.app.aml.domain.enums.RuleStatus;
import com.app.aml.feature.ruleengine.entity.TenantScenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TenantScenarioRepository extends JpaRepository<TenantScenario, UUID> {
    boolean existsByGlobalScenarioId(UUID globalScenarioId);
    List<TenantScenario> findByStatus(RuleStatus status);;
}