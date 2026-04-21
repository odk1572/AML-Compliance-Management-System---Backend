package com.app.aml.security.userDetails;


import com.app.aml.platform.entity.PlatformUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security adapter for Platform Users (Super Admins).
 * Wraps the database entity to provide authentication details.
 */
@RequiredArgsConstructor
public class PlatformUserDetails implements UserDetails {

    private final PlatformUser platformUser;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring Security expects roles to be prefixed with "ROLE_"
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + platformUser.getRole().name())
        );
    }

    @Override
    public String getPassword() {
        return platformUser.getPasswordHash(); // Assumes your entity uses this field name
    }

    @Override
    public String getUsername() {
        return platformUser.getEmail(); // We use email as the username for login
    }

    // You can expose the raw user object if you need it in your login controllers
    public PlatformUser getPlatformUser() {
        return platformUser;
    }

    // --- Account Status Checks ---

    @Override
    public boolean isAccountNonExpired() {
        return true; // Not used for MVP, default to true
    }

    @Override
    public boolean isAccountNonLocked() {
        return platformUser.isActive();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Not used for MVP, default to true
    }

    @Override
    public boolean isEnabled() {
        return !platformUser.isSysIsDeleted(); // Block access if soft-deleted
    }
}