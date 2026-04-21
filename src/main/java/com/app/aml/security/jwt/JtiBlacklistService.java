package com.app.aml.security.jwt;

import com.app.aml.multitenency.TenantContext;
import com.app.aml.security.repository.PlatformUserSessionRepository;
import com.app.aml.security.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant; // Added this
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

    private final Set<String> revokedJtiCache = ConcurrentHashMap.newKeySet();

    /**
     * Checks if a token has been revoked.
     * Called on EVERY request by the JwtAuthenticationFilter.
     */
    public boolean isTokenRevoked(String jti, String tenantId) {
        if (revokedJtiCache.contains(jti)) {
            return true;
        }

        boolean isRevoked;

        if (tenantId == null || tenantId.trim().isEmpty()) {
            isRevoked = platformSessionRepo.existsByJwtJtiAndIsRevokedTrue(jti);
        } else {
            String previousContext = TenantContext.getTenantId();
            try {
                TenantContext.setTenantId(tenantId);
                isRevoked = tenantSessionRepo.existsByJwtJtiAndIsRevokedTrue(jti);
            } finally {
                if (previousContext != null) {
                    TenantContext.setTenantId(previousContext);
                } else {
                    TenantContext.clear();
                }
            }
        }

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
        // We capture the moment of revocation for the audit trail
        Instant now = Instant.now();

        if (tenantId == null || tenantId.trim().isEmpty()) {
            // FIXED: Added 'now' as the second argument
            platformSessionRepo.revokeSessionByJti(jti, now);
        } else {
            String previousContext = TenantContext.getTenantId();
            try {
                TenantContext.setTenantId(tenantId);
                // FIXED: Added 'now' as the second argument
                tenantSessionRepo.revokeSessionByJti(jti, now);
            } finally {
                if (previousContext != null) {
                    TenantContext.setTenantId(previousContext);
                } else {
                    TenantContext.clear();
                }
            }
        }

        revokedJtiCache.add(jti);
        log.info("Token session {} has been successfully blacklisted.", jti);
    }
}