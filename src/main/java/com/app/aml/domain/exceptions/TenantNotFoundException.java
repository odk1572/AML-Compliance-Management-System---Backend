package com.app.aml.domain.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when the TenantSchemaResolver cannot find a physical
 * PostgreSQL schema mapped to a provided Tenant ID.
 */
@Getter
public class TenantNotFoundException extends ApplicationException {

    private final String tenantId;

    /**
     * Constructs the exception with the missing tenant ID.
     * Maps to a 404 NOT FOUND HTTP status.
     *
     * @param tenantId The invalid or missing tenant ID extracted from the context
     */
    public TenantNotFoundException(String tenantId) {
        super(
                String.format("Tenant schema could not be resolved for ID: '%s'. The tenant may be invalid, suspended, or deleted.", tenantId),
                "TENANT_NOT_FOUND",
                HttpStatus.NOT_FOUND
        );
        this.tenantId = tenantId;
    }
}