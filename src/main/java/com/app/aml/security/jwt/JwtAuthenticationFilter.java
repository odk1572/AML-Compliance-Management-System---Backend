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
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {

                Claims claims = tokenProvider.extractAllClaims(jwt);
                String userId = claims.getSubject();
                String jti = claims.getId();
                String role = claims.get("role", String.class);
                String tenantId = claims.get("tenantId", String.class);
                UUID userUuid = UUID.fromString(userId);
                if (tenantId == null) {
                    PlatformUser user = platformUserRepository.findById(userUuid).orElse(null);
                    if (user != null && user.isLocked()) {
                        log.warn("Blocking request: Platform User {} is locked.", userId);
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Your account has been locked.");
                        return;
                    }
                } else {
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

                if (jtiBlacklistService.isTokenRevoked(jti, tenantId)) {
                    log.warn("Attempted access with revoked JWT (jti: {}). Blocking request.", jti);
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session has been revoked or expired.");
                    return;
                }

                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        Collections.singletonList(authority)
                );

                Map<String, String> authDetails = new HashMap<>();
                authDetails.put("tenantId", tenantId);
                authentication.setDetails(authDetails);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Security context set for User: {}, Role: {}, Tenant: {}", userId, role, tenantId);
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);

    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}