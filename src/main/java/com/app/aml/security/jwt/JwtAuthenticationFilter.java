package com.app.aml.security.jwt;

import com.app.aml.feature.platformuser.entity.PlatformUser;
import com.app.aml.feature.platformuser.repository.PlatformUserRepository;
import com.app.aml.feature.tenantuser.entity.TenantUser;
import com.app.aml.feature.tenantuser.repository.TenantUserRepository;
import com.app.aml.multitenency.TenantContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Intercepts incoming HTTP requests to validate JWT Bearer tokens.
 * Extracts user identity and tenant routing data, verifies session validity,
 * and populates the Spring SecurityContext.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final JtiBlacklistService jtiBlacklistService;
    private final PlatformUserRepository platformUserRepository;
    private final TenantUserRepository tenantUserRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            // 1. Extract the JWT from the Authorization header
            String jwt = getJwtFromRequest(request);

            // 2. Cryptographically validate the token (checks signature & expiration)
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {

                Claims claims = tokenProvider.extractAllClaims(jwt);
                String userId = claims.getSubject();
                String jti = claims.getId();
                String role = claims.get("role", String.class);
                String tenantId = claims.get("tenantId", String.class);
                UUID userUuid = UUID.fromString(userId);

                // 3. CHECK LOCK STATUS FOR BOTH PLATFORM AND TENANT USERS
                if (tenantId == null) {
                    PlatformUser user = platformUserRepository.findById(userUuid).orElse(null);
                    if (user != null && user.isLocked()) {
                        log.warn("Blocking request: Platform User {} is locked.", userId);
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Your account has been locked.");
                        return;
                    }
                } else {
                    // Temporarily set TenantContext to query the correct schema, then clear it
                    TenantContext.setTenantId(tenantId);
                    try {
                        TenantUser tenantUser = tenantUserRepository.findById(userUuid).orElse(null);
                        if (tenantUser != null && tenantUser.isLocked()) {
                            log.warn("Blocking request: Tenant User {} is locked.", userId);
                            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Your account has been locked.");
                            return;
                        }
                    } finally {
                        TenantContext.clear();
                    }
                }

                // 4. Database Check: Ensure the session hasn't been revoked by an Admin
                if (jtiBlacklistService.isTokenRevoked(jti, tenantId)) {
                    log.warn("Attempted access with revoked JWT (jti: {}). Blocking request.", jti);
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session has been revoked or expired.");
                    return; // Halt the filter chain immediately
                }

                // 5. Build the Authentication object
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        Collections.singletonList(authority)
                );

                // 6. Attach the tenantId to the Authentication details
                Map<String, String> authDetails = new HashMap<>();
                authDetails.put("tenantId", tenantId);
                authentication.setDetails(authDetails);

                // 7. Set the Context
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Security context set for User: {}, Role: {}, Tenant: {}", userId, role, tenantId);
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        // 8. Continue to the next filter (usually TenantContextFilter)
        filterChain.doFilter(request, response);
    }

    /**
     * Helper to extract the raw token string from the HTTP header.
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}