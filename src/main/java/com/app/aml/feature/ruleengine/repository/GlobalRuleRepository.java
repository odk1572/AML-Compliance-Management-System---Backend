package com.app.aml.feature.ruleengine.repository;

import com.app.aml.feature.ruleengine.entity.GlobalRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GlobalRuleRepository extends JpaRepository<GlobalRule, UUID> {
   Optional<GlobalRule> findByIdAndSysIsDeletedFalse(UUID ruleId);
   boolean existsByRuleNameAndSysIsDeletedFalse(String ruleName);
   Page<GlobalRule> findAllBySysIsDeletedFalse(Pageable pageable);
   boolean existsByIdAndSysIsDeletedFalse(UUID ruleId);
   @Query(value = """
            SELECT 
                CAST(gr.id AS varchar) as id, 
                gr.rule_name as "ruleName", 
                gr.severity as severity, 
                gr.base_risk_score as "baseRiskScore",
                0 as "alertCount" 
            FROM common_schema.global_rules gr 
            WHERE gr.sys_is_deleted = false
            """,
           countQuery = "SELECT count(id) FROM common_schema.global_rules WHERE sys_is_deleted = false",
           nativeQuery = true)
   Page<Map<String, Object>> findAllRulesWithAlertCounts(Pageable pageable);

}