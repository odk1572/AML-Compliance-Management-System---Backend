package com.app.aml.multitenency;


import com.app.aml.tenant.entity.Tenant;
import com.app.aml.tenant.repository.TenantRepository;
import com.app.aml.security.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service responsible for suspending a tenant.
 * Enforces the 5-year regulatory data retention requirement by
 * suspending access rather than dropping the physical PostgreSQL schema.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantSchemaDeactivator {

    private final TenantRepository tenantRepository;
    private final UserSessionRepository userSessionRepository;

    // Injected to ensure the suspended tenant is cleared from fast-lookup memory
    private final TenantSchemaResolver tenantSchemaResolver;

    /**
     * Marks the tenant as SUSPENDED and instantly revokes all active JWT sessions.
     * * @param tenantId The UUID of the target tenant
     */
    @Transactional
    public void deactivate(String tenantId) {
        log.warn("Initiating deactivation sequence for tenantId: {}", tenantId);

        // ====================================================================
        // STEP 1: Operate in the Common Schema (Platform Level)
        // ====================================================================
        UUID id = UUID.fromString(tenantId);
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found in platform registry"));

        tenant.setStatus("SUSPENDED");
        tenantRepository.save(tenant);

        // Clear the cache so future requests don't accidentally resolve this tenant as active
        tenantSchemaResolver.evict(tenantId);

        // ====================================================================
        // STEP 2: Context Switch to the Tenant Schema (Bank Level)
        // ====================================================================
        // We must remember the Super Admin's current context so we can restore it safely.
        String previousTenantContext = TenantContext.getTenantId();

        try {
            // Force the DataSource to route the next queries into the target bank's schema
            TenantContext.setTenantId(tenantId);

            // Trigger a bulk update in the isolated schema:
            // UPDATE user_sessions SET is_revoked = true, revoked_at = NOW() WHERE is_revoked = false
            int revokedSessions = userSessionRepository.revokeAllActiveSessions();

            log.info("Severed {} active user sessions for tenant: {}", revokedSessions, tenant.getTenantCode());

        } finally {
            // ====================================================================
            // STEP 3: Restore Original Context (CRITICAL)
            // ====================================================================
            if (previousTenantContext != null) {
                TenantContext.setTenantId(previousTenantContext);
            } else {
                TenantContext.clear();
            }
        }

        log.info("Deactivation complete. Schema '{}' retained for 5-year regulatory compliance.", tenant.getSchemaName());
    }
}