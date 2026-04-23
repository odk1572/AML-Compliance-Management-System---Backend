package com.app.aml.feature.tenantuser.repository;

import com.app.aml.feature.tenantuser.entity.TenantUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantUserRepository extends JpaRepository<TenantUser, UUID> {
    Optional<TenantUser> findByEmail(String email);
    Optional<TenantUser> findByEmailIgnoreCase(String email);
}