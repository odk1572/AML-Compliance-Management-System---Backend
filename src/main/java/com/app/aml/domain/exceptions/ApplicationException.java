package com.app.aml.domain.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class ApplicationException extends RuntimeException {

    //  private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final HttpStatus httpStatus;

    public ApplicationException(String message,
                                String errorCode,
                                HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}