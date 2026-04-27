package com.app.aml.feature.tenantuser.repository;

import com.app.aml.feature.tenantuser.entity.TenantUser;
import com.app.aml.security.rbac.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantUserRepository extends JpaRepository<TenantUser, UUID> {
    Optional<TenantUser> findByEmail(String email);
    @Query(value = "SELECT * FROM tenant_users WHERE id = :id AND sys_is_deleted = true", nativeQuery = true)
    Optional<TenantUser> findDeletedById(@Param("id") UUID id);
    Optional<TenantUser> findByEmailIgnoreCase(String email);
    Optional<TenantUser> findByEmailAndSysIsDeletedFalse(String email);

    Page<TenantUser> findByRoleAndSysIsDeletedFalse(Role role, Pageable pageable);
    Optional<TenantUser> findByEmailIgnoreCaseAndSysIsDeletedFalse(String email);

    Optional<TenantUser> findByIdAndSysIsDeletedFalse(UUID id);


    Page<TenantUser> findAllBySysIsDeletedFalse(Pageable pageable);

    boolean existsByEmailAndSysIsDeletedFalse(String email);

    boolean existsByEmployeeIdAndSysIsDeletedFalse(String employeeId);
}