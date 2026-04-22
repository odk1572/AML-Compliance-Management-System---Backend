package com.app.aml.security.userDetails;


import com.app.aml.feature.tenantuser.entity.TenantUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security adapter for Bank Employees (Bank Admins & Compliance Officers).
 */
@RequiredArgsConstructor
public class TenantUserDetails implements UserDetails {

    private final TenantUser tenantUser;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + tenantUser.getRole().name())
        );
    }

    @Override
    public String getPassword() {
        return tenantUser.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return tenantUser.getEmail();
    }

    public TenantUser getTenantUser() {
        return tenantUser;
    }

    // --- Account Status Checks ---

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !tenantUser.isLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !tenantUser.isSysIsDeleted();
    }
}
