package com.app.aml.feature.ruleengine.repository;

import com.app.aml.feature.ruleengine.entity.TenantScenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface TenantScenarioRepository extends JpaRepository<TenantScenario, UUID> {
}