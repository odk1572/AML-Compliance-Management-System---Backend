package com.app.aml.feature.ruleengine.repository;

import com.app.aml.feature.ruleengine.entity.GlobalRuleCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GlobalRuleConditionRepository extends JpaRepository<GlobalRuleCondition, UUID> {
    List<GlobalRuleCondition> findByRuleIdOrderByConditionSequenceAsc(UUID ruleId);

}