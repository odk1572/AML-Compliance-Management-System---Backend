package com.app.aml.feature.ruleengine.repository;

import com.app.aml.feature.ruleengine.entity.GlobalScenarioRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GlobalScenarioRuleRepository extends JpaRepository<GlobalScenarioRule, UUID> {

    Optional<GlobalScenarioRule> findByScenarioIdAndRuleId(UUID scenarioId, UUID ruleId);
    boolean existsByScenarioIdAndRuleId(UUID scenarioId, UUID ruleId);
    List<GlobalScenarioRule> findByScenarioIdOrderByPriorityOrderAsc(UUID scenarioId);
}