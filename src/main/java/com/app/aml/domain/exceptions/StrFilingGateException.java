package com.app.aml.domain.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class StrFilingGateException extends ApplicationException {

    private final String caseId;


    public StrFilingGateException(String caseId) {
        super(
                String.format("Cannot file STR for Case ID '%s'. Regulatory rules mandate that an investigation note must be added prior to filing.", caseId),
                "MISSING_INVESTIGATION_NOTE",
                HttpStatus.UNPROCESSABLE_ENTITY
        );
        this.caseId = caseId;
    }
}