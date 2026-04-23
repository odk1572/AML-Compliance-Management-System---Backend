package com.app.aml.feature.tenant.repository;

import com.app.aml.feature.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    boolean existsByTenantCode(String tenantCode);
    boolean existsBySchemaName(String schemaName);
    Optional<Tenant> findByTenantCode(String tenantCode);
    Optional<Tenant> findByContactEmail(String email);
    Optional<Tenant> findBySchemaName(String discoveredSchema);
}