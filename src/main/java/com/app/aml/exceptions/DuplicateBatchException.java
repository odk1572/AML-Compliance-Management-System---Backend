package com.app.aml.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class DuplicateBatchException extends ApplicationException {

    private final String fileHash;


    public DuplicateBatchException(String fileHash) {
        super(
                String.format("A transaction batch with the SHA-256 hash '%s' has already been processed. Duplicate uploads are rejected.", fileHash),
                "DUPLICATE_BATCH_FILE",
                HttpStatus.CONFLICT
        );
        this.fileHash = fileHash;
    }
}