package com.app.aml.security.userDetails;

import com.app.aml.multitenency.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Primary // This tells Spring: "Use this one if there is an ambiguity"
@RequiredArgsConstructor
public class UnifiedUserDetailsService implements UserDetailsService {

    private final PlatformUserDetailsService platformUserDetailsService;
    private final TenantUserDetailsService tenantUserDetailsService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // If no tenant is set in context, we assume it's a Platform/SuperAdmin login
        if (TenantContext.getTenantId() == null) {
            return platformUserDetailsService.loadUserByUsername(username);
        }
        // Otherwise, look in the tenant schema
        return tenantUserDetailsService.loadUserByUsername(username);
    }
}