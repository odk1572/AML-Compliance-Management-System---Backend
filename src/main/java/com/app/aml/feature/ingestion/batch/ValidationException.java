package com.app.aml.feature.ingestion.batch;


import lombok.Getter;

@Getter
public class ValidationException extends RuntimeException {
    private final int row;
    private final String column;

    public ValidationException(int row, String column, String message) {
        super(message);
        this.row = row;
        this.column = column;
    }
}