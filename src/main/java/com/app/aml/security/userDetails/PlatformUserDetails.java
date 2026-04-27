package com.app.aml.security.userDetails;


import com.app.aml.feature.platformuser.entity.PlatformUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

@RequiredArgsConstructor
public class PlatformUserDetails implements UserDetails {

    private final PlatformUser platformUser;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + platformUser.getRole().name())
        );
    }

    @Override
    public String getPassword() {
        return platformUser.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return platformUser.getEmail();
    }

    public PlatformUser getPlatformUser() {
        return platformUser;
    }


    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !platformUser.isLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !platformUser.isSysIsDeleted();
    }
}