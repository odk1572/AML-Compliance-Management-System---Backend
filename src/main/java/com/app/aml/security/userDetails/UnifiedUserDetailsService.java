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
        if (TenantContext.getTenantId() == null) {
            try {
                return platformUserDetailsService.loadUserByUsername(email);
            } catch (UsernameNotFoundException e) {
                return discoverTenantAndLoad(email);
            }
        }

        return tenantUserDetailsService.loadUserByUsername(email);
    }

    private UserDetails discoverTenantAndLoad(String email) {
        Tenant tenant = tenantRepository.findByContactEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found in any tenant."));

        TenantContext.setTenantId(tenant.getSchemaName());
        entityManager.clear();

        return tenantUserDetailsService.loadUserByUsername(email);
    }
}