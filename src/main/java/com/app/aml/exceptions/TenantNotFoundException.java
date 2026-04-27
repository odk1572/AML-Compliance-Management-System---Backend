package com.app.aml.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class TenantNotFoundException extends ApplicationException {

    private final String tenantId;

    public TenantNotFoundException(String tenantId) {
        super(
                String.format("Tenant schema could not be resolved for ID: '%s'. The tenant may be invalid, suspended, or deleted.", tenantId),
                "TENANT_NOT_FOUND",
                HttpStatus.NOT_FOUND
        );
        this.tenantId = tenantId;
    }
}