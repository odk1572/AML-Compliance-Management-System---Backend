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

@Slf4j
@Service
@RequiredArgsConstructor
public class JtiBlacklistService {

    private final PlatformUserSessionRepository platformSessionRepo;
    private final UserSessionRepository tenantSessionRepo;

    private final Set<String> revokedJtiCache = ConcurrentHashMap.newKeySet();

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

    @Transactional
    public void blacklistToken(String jti, String tenantId) {
        Instant now = Instant.now();

        if (tenantId == null || tenantId.trim().isEmpty()) {
            platformSessionRepo.revokeSessionByJti(jti, now);
        } else {
            String previousContext = TenantContext.getTenantId();
            try {
                TenantContext.setTenantId(tenantId);
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