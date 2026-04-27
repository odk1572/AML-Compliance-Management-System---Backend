package com.app.aml.exceptions;

import org.springframework.http.HttpStatus;

public class UnauthorizedTenantAccessException extends ApplicationException {

    public UnauthorizedTenantAccessException(String message) {
        super(
                message,
                "TENANT_ACCESS_DENIED",
                HttpStatus.FORBIDDEN
        );
    }
}