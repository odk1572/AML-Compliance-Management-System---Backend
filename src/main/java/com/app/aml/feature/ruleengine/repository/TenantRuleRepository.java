package com.app.aml.feature.ruleengine.repository;

import com.app.aml.feature.ruleengine.entity.TenantRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRuleRepository extends JpaRepository<TenantRule, UUID> {

    List<TenantRule> findByTenantScenarioIdAndIsActiveTrue(UUID tenantScenarioId);
    List<TenantRule> findByTenantScenarioId(UUID tenantScenarioId);
    Optional<TenantRule> findByRuleCode(String tenantRuleCode);

}