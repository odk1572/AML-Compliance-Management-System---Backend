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

/**
 * Loads Bank Employees from their specific isolated schema.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantUserDetailsService implements UserDetailsService {

    private final TenantUserRepository tenantUserRepository;

    @Override
    @Transactional(readOnly = true) // This starts a new transaction in the isolated schema context
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // 1. HARD GUARD: We cannot look up a bank user without knowing the bank.
        if (!TenantContext.isTenantSet()) {
            log.error("Attempted to load tenant user [{}] without an active TenantContext.", email);
            throw new AuthenticationServiceException("Tenant ID is required for bank login.");
        }

        String tenantId = TenantContext.getTenantId();
        log.debug("Attempting to load bank user [{}] from tenant schema [{}]", email, tenantId);

        // 2. Fetch the user from the isolated schema
        TenantUser user = tenantUserRepository.findByEmailAndSysIsDeletedFalse(email)
                .orElseThrow(() -> {
                    log.warn("Bank login failed: No user found with email {} in tenant {}", email, tenantId);
                    return new UsernameNotFoundException("Invalid credentials.");
                });

        return new TenantUserDetails(user);
    }
}