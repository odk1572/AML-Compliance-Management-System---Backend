package com.app.aml.domain.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a Compliance Officer attempts to file an STR
 * (Suspicious Transaction Report) for a case that lacks a required investigation note.
 * Maps to a 422 UNPROCESSABLE ENTITY HTTP status.
 */
@Getter
public class StrFilingGateException extends ApplicationException {

    private final String caseId;

    /**
     * Constructs the exception with the specific case ID that failed the gate check.
     *
     * @param caseId The ID of the case attempting to be filed
     */
    public StrFilingGateException(String caseId) {
        super(
                String.format("Cannot file STR for Case ID '%s'. Regulatory rules mandate that an investigation note must be added prior to filing.", caseId),
                "MISSING_INVESTIGATION_NOTE",
                HttpStatus.UNPROCESSABLE_ENTITY
        );
        this.caseId = caseId;
    }
}