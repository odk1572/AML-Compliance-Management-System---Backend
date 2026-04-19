package com.app.aml.domain.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown during the transaction ingestion process when a file's
 * SHA-256 hash matches a batch that has already been uploaded by the tenant.
 * Maps to a 409 CONFLICT HTTP status.
 */
@Getter
public class DuplicateBatchException extends ApplicationException {

    private final String fileHash;

    /**
     * Constructs the exception with the duplicate hash.
     *
     * @param fileHash The SHA-256 hash of the rejected file
     */
    public DuplicateBatchException(String fileHash) {
        super(
                String.format("A transaction batch with the SHA-256 hash '%s' has already been processed. Duplicate uploads are rejected.", fileHash),
                "DUPLICATE_BATCH_FILE",
                HttpStatus.CONFLICT
        );
        this.fileHash = fileHash;
    }
}