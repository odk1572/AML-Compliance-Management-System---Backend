package com.app.aml.multitenency;


import com.app.aml.enums.TenantStatus;
import com.app.aml.feature.tenant.entity.Tenant;
import com.app.aml.feature.tenant.repository.TenantRepository;
import com.app.aml.security.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class TenantSchemaDeactivator {

    private final TenantRepository tenantRepository;
    private final UserSessionRepository userSessionRepository;

    private final TenantSchemaResolver tenantSchemaResolver;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deactivate(String tenantId) {
        log.warn("Initiating deactivation sequence for tenantId: {}", tenantId);

        UUID id = UUID.fromString(tenantId);
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found in platform registry"));

        tenant.setStatus(TenantStatus.SUSPENDED);
        tenantRepository.save(tenant);

        tenantRepository.flush();

        tenantSchemaResolver.evict(tenantId);

        String previousTenantContext = TenantContext.getTenantId();

        try {
            TenantContext.setTenantId(tenantId);

            int revokedSessions = userSessionRepository.revokeAllActiveSessions();

            log.info("Severed {} active user sessions for tenant: {}", revokedSessions, tenant.getTenantCode());

        } finally {
            if (previousTenantContext != null) {
                TenantContext.setTenantId(previousTenantContext);
            } else {
                TenantContext.clear();
            }
        }

        log.info("Deactivation complete. Schema '{}' retained for 5-year regulatory compliance.", tenant.getSchemaName());
    }
}