package com.app.aml.multitenency;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class enforcing strict tenant boundaries at the application layer.
 * Acts as a fail-safe secondary wall alongside the Database Schema routing.
 */
@Slf4j
public class TenantBoundaryValidator {

    // Private constructor to prevent instantiation
    private TenantBoundaryValidator() {}

    /**
     * Asserts that the provided Tenant ID matches the current thread's TenantContext.
     * Must be called in service methods before returning or mutating sensitive data.
     *
     * @param entityTenantId The tenant ID associated with the requested resource
     * @throws UnauthorizedTenantAccessException if a mismatch is detected
     */
    public static void assertBelongsToCurrentTenant(String entityTenantId) {
        String currentContextTenantId = TenantContext.getTenantId();

        // 1. If context is null, it means the request bypassed the security filter (Severe Error)
        if (currentContextTenantId == null) {
            log.error("SECURITY ALERT: Attempted to validate boundary with an empty Tenant Context.");
            throw new UnauthorizedTenantAccessException("Active tenant context is missing.");
        }

        // 2. If the resource doesn't have a tenant ID, something is structurally wrong with the data
        if (entityTenantId == null) {
            log.error("SECURITY ALERT: Attempted to validate an entity missing a Tenant ID.");
            throw new UnauthorizedTenantAccessException("Target resource is missing tenant association.");
        }

        // 3. The Core Boundary Check
        if (!currentContextTenantId.equals(entityTenantId)) {
            log.error("SECURITY BREACH ATTEMPT: Context Tenant [{}] attempted to access Resource Tenant [{}]",
                    currentContextTenantId, entityTenantId);

            throw new UnauthorizedTenantAccessException("Access denied: Tenant boundary violation detected.");
        }
    }

    /**
     * Custom Exception for Boundary Violations.
     * In a real application, you might move this to your 'exception' package.
     */
    public static class UnauthorizedTenantAccessException extends RuntimeException {
        public UnauthorizedTenantAccessException(String message) {
            super(message);
        }
    }
}