package com.app.aml.security.jwt;


import com.app.aml.multitenency.TenantContext;
import com.app.aml.security.repository.PlatformUserSessionRepository;
import com.app.aml.security.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service responsible for tracking and validating revoked JWTs.
 * Uses a dual-layer approach (In-Memory Cache + Database) to ensure high performance
 * during the Spring Security filter chain.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JtiBlacklistService {

    private final PlatformUserSessionRepository platformSessionRepo;
    private final UserSessionRepository tenantSessionRepo;

    // MVP Fast-Cache: Stores ONLY revoked JTIs to prevent a DB hit on every single API request.
    // In a distributed Year-2 environment, this would be replaced with Redis.
    private final Set<String> revokedJtiCache = ConcurrentHashMap.newKeySet();

    /**
     * Checks if a token has been revoked.
     * Called on EVERY request by the JwtAuthenticationFilter.
     */
    public boolean isTokenRevoked(String jti, String tenantId) {
        // 1. FAST PATH: Check the in-memory blacklist cache first
        if (revokedJtiCache.contains(jti)) {
            return true;
        }

        // 2. SLOW PATH: Database Validation
        boolean isRevoked;

        if (tenantId == null || tenantId.trim().isEmpty()) {
            // Super Admin Token -> Check the common schema
            isRevoked = platformSessionRepo.existsByJwtJtiAndIsRevokedTrue(jti);
        } else {
            // Bank Employee Token -> We MUST manually switch the schema context here
            // because this filter runs before the TenantContextFilter has a chance to!
            String previousContext = TenantContext.getTenantId();
            try {
                TenantContext.setTenantId(tenantId);
                isRevoked = tenantSessionRepo.existsByJwtJtiAndIsRevokedTrue(jti);
            } finally {
                // Safely restore context
                if (previousContext != null) {
                    TenantContext.setTenantId(previousContext);
                } else {
                    TenantContext.clear();
                }
            }
        }

        // 3. Cache the result: If the DB says it is revoked, add it to the fast-cache.
        // Once a token is revoked, it stays revoked, so caching it is perfectly safe.
        if (isRevoked) {
            revokedJtiCache.add(jti);
            log.trace("JTI {} added to memory blacklist cache.", jti);
        }

        return isRevoked;
    }

    /**
     * Actively revokes a session. Called when a user logs out, or an Admin suspends an account.
     */
    @Transactional
    public void blacklistToken(String jti, String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            platformSessionRepo.revokeSessionByJti(jti);
        } else {
            String previousContext = TenantContext.getTenantId();
            try {
                TenantContext.setTenantId(tenantId);
                tenantSessionRepo.revokeSessionByJti(jti);
            } finally {
                if (previousContext != null) {
                    TenantContext.setTenantId(previousContext);
                } else {
                    TenantContext.clear();
                }
            }
        }

        // Instantly add to memory cache so the very next request is blocked
        revokedJtiCache.add(jti);
        log.info("Token session {} has been successfully blacklisted.", jti);
    }
}
