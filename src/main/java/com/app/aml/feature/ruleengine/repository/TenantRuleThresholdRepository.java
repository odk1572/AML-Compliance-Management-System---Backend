package com.app.aml.feature.ruleengine.repository;

import com.app.aml.feature.ruleengine.entity.TenantRuleThreshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TenantRuleThresholdRepository extends JpaRepository<TenantRuleThreshold, UUID> {

    List<TenantRuleThreshold> findByTenantRuleId(UUID tenantRuleId);
}