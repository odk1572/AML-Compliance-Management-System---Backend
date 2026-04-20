package com.app.aml.domain.exceptions;


import org.springframework.http.HttpStatus;

public class TenantSuspendedException extends ApplicationException {

    public TenantSuspendedException(String tenantId) {
        super(
                String.format("Tenant [%s] is currently suspended or inactive.", tenantId),
                "TENANT_SUSPENDED",
                HttpStatus.FORBIDDEN
        );
    }
}
