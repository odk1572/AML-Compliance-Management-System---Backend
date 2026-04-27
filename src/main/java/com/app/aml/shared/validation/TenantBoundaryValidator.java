package com.app.aml.shared.validation;

import com.app.aml.multitenency.TenantContext;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class TenantBoundaryValidator {

    private TenantBoundaryValidator() {}


    public static void assertBelongsToCurrentTenant(String entityTenantId) {
        String currentContextTenantId = TenantContext.getTenantId();

        if (currentContextTenantId == null) {
            log.error("SECURITY ALERT: Attempted to validate boundary with an empty Tenant Context.");
            throw new UnauthorizedTenantAccessException("Active tenant context is missing.");
        }

        if (entityTenantId == null) {
            log.error("SECURITY ALERT: Attempted to validate an entity missing a Tenant ID.");
            throw new UnauthorizedTenantAccessException("Target resource is missing tenant association.");
        }

        if (!currentContextTenantId.equals(entityTenantId)) {
            log.error("SECURITY BREACH ATTEMPT: Context Tenant [{}] attempted to access Resource Tenant [{}]",
                    currentContextTenantId, entityTenantId);

            throw new UnauthorizedTenantAccessException("Access denied: Tenant boundary violation detected.");
        }
    }


    public static class UnauthorizedTenantAccessException extends RuntimeException {
        public UnauthorizedTenantAccessException(String message) {
            super(message);
        }
    }
}