package com.app.aml.security.userDetails;

import com.app.aml.feature.tenant.entity.Tenant;
import com.app.aml.feature.tenant.repository.TenantRepository;
import com.app.aml.multitenency.TenantContext;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Primary
@RequiredArgsConstructor
public class UnifiedUserDetailsService implements UserDetailsService {

    private final PlatformUserDetailsService platformUserDetailsService;
    private final TenantUserDetailsService tenantUserDetailsService;
    private final TenantRepository tenantRepository;
    private final EntityManager entityManager;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. If no header is provided, check if it's a Platform User first
        if (TenantContext.getTenantId() == null) {
            try {
                return platformUserDetailsService.loadUserByUsername(email);
            } catch (UsernameNotFoundException e) {
                // Not a platform user? Let's try to "Discover" their tenant by email
                return discoverTenantAndLoad(email);
            }
        }

        // 2. If a header WAS provided, proceed with the Tenant service
        return tenantUserDetailsService.loadUserByUsername(email);
    }

    private UserDetails discoverTenantAndLoad(String email) {
        // Look in common_schema.tenants to see which bank this email belongs to
        Tenant tenant = tenantRepository.findByContactEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found in any tenant."));

        // Switch context to the discovered tenant's schema
        TenantContext.setTenantId(tenant.getSchemaName());

        // CRITICAL: Clear Hibernate's cache to force it to drop common_schema metadata
        // and fetch fresh metadata for the tenant_users table in the new schema.
        entityManager.clear();

        return tenantUserDetailsService.loadUserByUsername(email);
    }
}