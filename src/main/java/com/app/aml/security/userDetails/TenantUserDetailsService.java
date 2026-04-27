package com.app.aml.security.userDetails;

import com.app.aml.multitenency.TenantContext;
import com.app.aml.feature.tenantuser.entity.TenantUser;
import com.app.aml.feature.tenantuser.repository.TenantUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantUserDetailsService implements UserDetailsService {

    private final TenantUserRepository tenantUserRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        if (!TenantContext.isTenantSet()) {
            log.error("Attempted to load tenant user [{}] without an active TenantContext.", email);
            throw new AuthenticationServiceException("Tenant ID is required for bank login.");
        }

        String tenantId = TenantContext.getTenantId();
        log.debug("Attempting to load bank user [{}] from tenant schema [{}]", email, tenantId);

        TenantUser user = tenantUserRepository.findByEmailAndSysIsDeletedFalse(email)
                .orElseThrow(() -> {
                    log.warn("Bank login failed: No user found with email {} in tenant {}", email, tenantId);
                    return new UsernameNotFoundException("Invalid credentials.");
                });

        return new TenantUserDetails(user);
    }
}