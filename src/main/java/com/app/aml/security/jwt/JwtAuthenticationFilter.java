package com.app.aml.security.jwt;

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
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
    private final JtiBlacklistService jtiBlacklistService; // Checks the database for revoked jti

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

                // 3. Database Check: Ensure the session hasn't been revoked by an Admin
                // This service will handle routing to common_schema or tenant_schema internally
                if (jtiBlacklistService.isTokenRevoked(jti, tenantId)) {
                    log.warn("Attempted access with revoked JWT (jti: {}). Blocking request.", jti);
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session has been revoked or expired.");
                    return; // Halt the filter chain immediately
                }

                // 4. Build the Authentication object
                // We map the string role (e.g., "COMPLIANCE_OFFICER") into Spring's GrantedAuthority format
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId,
                        null, // Credentials are null since JWT is already verified
                        Collections.singletonList(authority)
                );

                // 5. Attach the tenantId to the Authentication details
                // This is CRUCIAL so the TenantContextFilter (which runs next) can grab it
                Map<String, String> authDetails = new HashMap<>();
                authDetails.put("tenantId", tenantId);
                authentication.setDetails(authDetails);

                // Add standard web details (IP address, Session ID)
                // authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 6. Set the Context
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Security context set for User: {}, Role: {}, Tenant: {}", userId, role, tenantId);
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
            // We do not throw exceptions here. If authentication fails, the context remains empty.
            // Spring Security's AuthenticationEntryPoint will automatically return a 401 response later.
        }

        // 7. Continue to the next filter (usually TenantContextFilter)
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