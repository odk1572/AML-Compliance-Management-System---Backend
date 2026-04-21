package com.app.aml.feature.ruleengine.repository;

import com.app.aml.feature.ruleengine.entity.GlobalScenarioRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface GlobalScenarioRuleRepository extends JpaRepository<GlobalScenarioRule, UUID> {
}