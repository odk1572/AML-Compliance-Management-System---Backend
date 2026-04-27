package com.app.aml.exceptions;

import org.springframework.http.HttpStatus;

public class BusinessRuleException extends ApplicationException {

    public BusinessRuleException(String message) {
        super(message, "BUSINESS_RULE_VIOLATION", HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
