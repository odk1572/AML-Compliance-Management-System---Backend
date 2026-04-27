package com.app.aml.security.userDetails;


import com.app.aml.multitenency.TenantContext;
import com.app.aml.feature.platformuser.entity.PlatformUser;
import com.app.aml.feature.platformuser.repository.PlatformUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformUserDetailsService implements UserDetailsService {

    private final PlatformUserRepository platformUserRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Attempting to load platform user by email: {}", email);
        TenantContext.clear();

        PlatformUser user = platformUserRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Platform login failed: No user found with email {}", email);
                    return new UsernameNotFoundException("Invalid credentials.");
                });

        return new PlatformUserDetails(user);
    }
}